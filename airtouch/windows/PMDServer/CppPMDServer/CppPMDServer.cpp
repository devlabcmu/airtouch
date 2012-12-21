#include <stdio.h>
#include <tchar.h>
#include <assert.h>

#include "simplewinsock.h"
#include "strutils.h"
#include"simplepmd.h"

#include <opencv/cxcore.h>
#include <opencv/highgui.h>

using namespace std;

#define BUFSIZE 512

PMDData _pmdData;
PMDFingerData _fingerData;
PMDRequest _fromClient;

PMDDataDescription _pmdDataDescription;
PMDHandle _pmdHandle;
unsigned int _pmdFlags[PMDIMAGESIZE];
char _pmdErrorBuffer[BUFSIZE];

/* function declarations */
// PMD
HRESULT updateCameraData();

// UI
void welcomeMessage();
IplImage* _ocvFrame;
int _ocvFrameStep;

// Networking
bool communicateWithClient(SOCKET* hClient);

void welcomeMessage()
{
	// add welcome here
	cout << "PMDServer sends data from pmd camera or a file" << endl;
	cout << "Usage: PMDServer.exe to read from camera" << endl;
	cout << "Usage: PMDServer.exe [filename.pmd] to read from .pmd file" << endl;
}

void updateUI()
{
	unsigned char * currentRow = 0;
    unsigned char * imgPtr = (unsigned char *) _ocvFrame->imageData;
	float * dataPtr = _pmdData.buffer;
	int step = _ocvFrame->nChannels;
	for (int y = 0; y < PMDNUMROWS; ++y)
    {
		currentRow = &imgPtr[y * _ocvFrameStep];
		for (int x = 0; x < PMDNUMCOLS; ++x, currentRow += step, ++dataPtr)
        {
			unsigned char val;
            // Clamp at 1 meters and scale the values in between to fit the image
            if (*dataPtr > 1.0f)
            {
                val = 0;
            }
            else
            {
                val = 255 - (unsigned char) (*dataPtr * 255.0f);
            }
			currentRow[0] = val;
			currentRow[1] = val;
			currentRow[2] = val;
        }
    }

    cvFlip (_ocvFrame, _ocvFrame, 0);
	cvCircle(_ocvFrame, cvPoint(cvRound(_pmdData.fingerX), 
		PMDNUMROWS - cvRound(_pmdData.fingerY)), 
		10, 
		CV_RGB(255,0,0));
    // Display the image
    cvShowImage ("Server", _ocvFrame);
	
}

//
// PMD
//

// initializes the PMD sensor
// returns error code


HRESULT updateCameraData()
{
	int res = pmdUpdate (_pmdHandle);
	if (res != PMD_OK)
	{
		pmdGetLastError (_pmdHandle, _pmdErrorBuffer, 128);
		cout << "Could not transfer data:" <<  _pmdErrorBuffer << endl;
		pmdClose (_pmdHandle);
		return -1;
	}

	res = pmdGetSourceDataDescription (_pmdHandle, &_pmdDataDescription);
	if (res != PMD_OK)
	{
		pmdGetLastError (_pmdHandle, _pmdErrorBuffer, BUFSIZ);
		cout << "Could get data description:" << _pmdErrorBuffer << endl;
		pmdClose (_pmdHandle);
		return -1;
	}

	cout << "retrieved source data description" << endl;  

	if (_pmdDataDescription.subHeaderType != PMD_IMAGE_DATA)
	{
		fprintf (stderr, "Source data is not an image!\n");
		pmdClose (_pmdHandle);
		return -1;
	}

	// make sure that the numrows and numcolumsn is same size as PMDIMAGESIZE
	assert(_pmdDataDescription.img.numColumns * _pmdDataDescription.img.numRows == PMDIMAGESIZE);

	res = pmdGetDistances (_pmdHandle, _pmdData.buffer, _pmdDataDescription.img.numColumns * _pmdDataDescription.img.numRows * sizeof (float));
	if (res != PMD_OK)
	{
		pmdGetLastError (_pmdHandle, _pmdErrorBuffer, 128);
		fprintf (stderr, "Could get distances: %s\n", _pmdErrorBuffer);
		pmdClose (_pmdHandle);
		return -1;
	}

	res = pmdGetFlags(_pmdHandle, _pmdFlags, PMDIMAGESIZE * 4);
	if (res != PMD_OK)
	{
		pmdGetLastError (_pmdHandle, _pmdErrorBuffer, 128);
		fprintf (stderr, "Could get distances: %s\n", _pmdErrorBuffer);
		pmdClose (_pmdHandle);
		return -1;
	}


	// find the x, y coordinate that has the highest value
	_pmdData.fingerX = 0;
	_pmdData.fingerY = 0;
	_pmdData.fingerZ = FLT_MAX;

	for(int y = 0; y < _pmdDataDescription.img.numRows; y++)
	{
		for(int x = 0; x < _pmdDataDescription.img.numColumns; x++)
		{
			int idx = y * _pmdDataDescription.img.numColumns + x;
			if(_pmdFlags[idx] & PMD_FLAG_INVALID)
			{
				_pmdData.buffer[idx] = 1;
			} else if(_pmdData.buffer[idx] < _pmdData.fingerZ)
			{
				_pmdData.fingerX = x;
				_pmdData.fingerY = y;
				_pmdData.fingerZ = _pmdData.buffer[idx];
			}
		}
	}

	_fingerData.fingerX = _pmdData.fingerX;
	_fingerData.fingerY = _pmdData.fingerY;
	_fingerData.fingerZ = _pmdData.fingerZ;

	return 0;
}



// Initializes connection to client and sends data.
// Returns when client disconnects
// Returns true if client sends a shutdown message.
bool communicateWithClient(SOCKET* hClient)
{
	bool result = true;
	// receive params about client 
	memset(_pmdData.buffer, 0, BUFSIZE);
	memset(_fromClient.buffer, 0, BUFSIZE);

	cout << "receiving initial handshake..." << endl;

	int bytesReceived = receiveData(*hClient, _fromClient.buffer, 512, false);

	// check input data
	if(!SUCCEEDED(result))
	{
		cout << "failed to receive data from client, disconnecting..." << endl;
		return true;
	}
	
	cout << "client first message: " << _fromClient.buffer  << "echoing..." << endl;


	// send a reply, simply echo for now
	HRESULT hr = sendData(*hClient, _fromClient.buffer, BUFSIZE, 0);
	if(!SUCCEEDED(hr)) return true;

	while(true)
	{
		// receive data from client
		// check first int, if 'd' then disconnect
		// if 'q' then shutdown
		bytesReceived = receiveData(*hClient, _fromClient.buffer, 512, false);
		if(bytesReceived == 0) return true;
		// write the rest of the data send to the screen
		cout << "received: " << (_fromClient.buffer) << endl;

		char command = _fromClient.buffer[0];

		if(command == 'q') return false;
		if(command == 'd') return true;


		// fill the current send data with data from the camera
		// send the data
		memset(&_pmdData, 0, sizeof(PMDData));
		hr = updateCameraData();

		if(!SUCCEEDED(hr)) return true;
		if(command == 'f')
		{
			// only send the finger data
			hr = sendData(*hClient, (char*)&_fingerData, sizeof(PMDFingerData), 0);	
		} else
		{
			hr = sendData(*hClient, (char*)&_pmdData, sizeof(PMDData), 0);	
		}
		
		// update the UI, just for kicks
		updateUI();
		cvWaitKey (1);

		if(!SUCCEEDED(hr)) return true;

	}

	// close client connection 

	return result;
}

int main(int argc, char* argv[])
{
	// welcome message
	welcomeMessage();
	
	// initialize opencv frame
	_ocvFrame = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), 8, 3);
	_ocvFrameStep = _ocvFrame->widthStep / sizeof(unsigned char);

	HRESULT hr = 0;

	// check if we have parameters, if so first param is filename
	if(argc > 1)
	{
		char* filename = argv[1];
		cout << "Reading data from file " << filename << endl;
		hr = initializePMDFromFile(&_pmdHandle,filename, _pmdErrorBuffer, BUFSIZE);
		if(!SUCCEEDED(hr)) return -1;
	} else 
	{
		initializePMD(&_pmdHandle, _pmdErrorBuffer, BUFSIZE);
	}
	

	
	WSADATA wsaData = {0};
	WORD wVer = MAKEWORD(2,2);

	// Step 1: initialize the socket
	hr = initWinsock(&wsaData, &wVer);
	if(!SUCCEEDED(hr)) return -1;

	cout << "Starting server on socket 10000..." << endl;

	SOCKET hSock  = {0};
	hSock = socket( AF_INET, SOCK_STREAM, IPPROTO_IP );
	if( hSock == INVALID_SOCKET ) { 
		cout << "Invalid socket, failed to create socket" << endl;
		return -1;
	}

	// step 3: bind socket
	sockaddr_in saListen = {0};
	saListen.sin_family      = PF_INET;    
	saListen.sin_port        = htons( 10000 );     
	saListen.sin_addr.s_addr = htonl( INADDR_ANY );  
	// bind socket's name 
	hr = bindSocket(&hSock, &saListen);
	if(!SUCCEEDED(hr)) return -1;

	// listen for incoming connections until force kill
	bool stayAlive = true;
	while( stayAlive )
	{
		cout << "Listening for connections on port 10000...." << endl;

		// step 4: listen for a client 
		SOCKET hClient;
		hr = getClientConnection(&hSock, &hClient);
		if(!SUCCEEDED(hr)) return -1;

		stayAlive = communicateWithClient(&hClient);
		closesocket( hClient );
		hClient = 0;
	}

	cvReleaseImage (&_ocvFrame);

	cout << "Shutting down the server" << endl;

	// close server socket 
	hr = closesocket( hSock );
	hSock = 0;
	if( hr == SOCKET_ERROR )  cout << "Error failed to close socket" << endl;


	// Release WinSock DLL 
	hr = WSACleanup();
	if( hr == SOCKET_ERROR ) cout << "Error cleaning up Winsock Library" << endl;
}