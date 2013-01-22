#include <stdio.h>
#include <tchar.h>
#include <assert.h>

#include "simplewinsock.h"
#include "strutils.h"
#include"airtouch.h"

#include <math.h>

#include <opencv/cxcore.h>
#include <opencv/highgui.h>
#include <opencv2/imgproc/imgproc.hpp>

using namespace cv;
using namespace std;

#define SSTR( x ) ( dynamic_cast< std::ostringstream & >( ( std::ostringstream() << std::dec << x ) ) ).str() 

#define BUFSIZE 512
#define BACKGROUND_THRESHOLD_STDEV 5

/* Globar vars */
// Sent over network
PMDData _pmdData;
PMDFingerData _fingerData;
PMDRequest _fromClient;

// PMD
float _pmdDistanceBuffer[PMDIMAGESIZE];
PMDDataDescription _pmdDataDescription;
PMDHandle _pmdHandle;
unsigned int _pmdFlags[PMDIMAGESIZE];
char _pmdErrorBuffer[BUFSIZE];

// Image processing
BackgroundSubtractionData _background;

/* function declarations */
// PMD
HRESULT updateCameraData();

// UI
void welcomeMessage();
IplImage* _ocvFrame;
int _ocvFrameStep;

// Image processing
void initializeBackgroundSubtraction();
void performBackgroundSubtraction();
void showBackgroundImage();
void findFingers();

// Random
void error(string msg);

// Networking
bool communicateWithClient(SOCKET* hClient);

void error(string msg)
{
	cout << msg << endl; 
	exit(EXIT_FAILURE);
}

//
// UI
//

void welcomeMessage()
{
	// add welcome here
	cout << "PMDServer sends data from pmd camera or a file" << endl;
	cout << "Usage: PMDServer.exe to read from camera" << endl;
	cout << "Usage: PMDServer.exe [filename.pmd] to read from .pmd file" << endl;
}

void updateUI()
{
	depthDataToImage(_pmdDistanceBuffer, (unsigned char *)  _ocvFrame->imageData, _ocvFrameStep, _ocvFrame->nChannels);
    cvFlip (_ocvFrame, _ocvFrame, 0);
	cvCircle(_ocvFrame, cvPoint(cvRound(_pmdData.fingerX), 
		PMDNUMROWS - cvRound(_pmdData.fingerY)), 
		10, 
		CV_RGB(255,0,0));
    // Display the image
    cvShowImage ("Server", _ocvFrame);
	
}


//
// Image Processing
//

void initializeBackgroundSubtraction()
{
	cout << "initializing background subtraction..." << endl;
	// for first 50 frames compute mean, standard deviation
	ZeroMemory(_background.means, _countof(_background.means) * sizeof(float));
	ZeroMemory(_background.stdevs, _countof(_background.means) * sizeof(float));
	// first get 50 frames
	float* frames[_numFramesForBackgroundSubtraction];
	float framesForAverage[PMDIMAGESIZE];
	ZeroMemory(framesForAverage, _countof(framesForAverage) * sizeof(float));

	// fill in the frames
	for (int i = 0; i < _numFramesForBackgroundSubtraction; i++)
	{
		float* frame = new float[PMDIMAGESIZE];
		frames[i] = frame;
		// fill the frame with data
		HRESULT hr = updateCameraData();

		if(!SUCCEEDED(hr)) error("Error: Failed to update camera data in initializeBackgroundSubtraction");
		
		memcpy_s(frame, PMDIMAGESIZE * sizeof(float), _pmdDistanceBuffer, PMDIMAGESIZE * sizeof(float));
	}

	// figure out how many frames to average over for each pixel (depends on valid data)
	for (int i = 0; i < _numFramesForBackgroundSubtraction; i++)
	{
		for(int j = 0; j < PMDIMAGESIZE; j++)
		{
			if(frames[i][j] != PMD_INVALID_DISTANCE) framesForAverage[j]++;
		}
	}
	// get averages
	for (int i = 0; i < _numFramesForBackgroundSubtraction; i++)
	{
		float* curFrame = frames[i];
		float* curBgRow = 0;
		float* curDataRow = 0;
		for(int y = 0; y < _pmdDataDescription.img.numRows; y++)
		{
			curBgRow = &_background.means[y* _pmdDataDescription.img.numColumns];
			curDataRow = &curFrame[y * _pmdDataDescription.img.numColumns];
			
			for(int x = 0; x < _pmdDataDescription.img.numColumns; x++)
			{
				if(curDataRow[x] == PMD_INVALID_DISTANCE) continue;
				int avI = y * _pmdDataDescription.img.numColumns + x;
				if(framesForAverage[avI] > 0)
					curBgRow[x] += curDataRow[x] / framesForAverage[avI];
				else
					curBgRow[x] = PMD_INVALID_DISTANCE;
			}
		}
	}
	
	// compute standard deviation for each frame
	for (int i = 0; i < _numFramesForBackgroundSubtraction; i++)
	{
		float* frame = frames[i];
		
		// fill the frame with data
		float* curMeanRow = 0;
		float* curStdevRow = 0;
		float* curDataRow = 0;
		for(int y = 0; y < _pmdDataDescription.img.numRows; y++)
		{
			curMeanRow = &_background.means[y* _pmdDataDescription.img.numColumns];
			curStdevRow = &_background.stdevs[y * _pmdDataDescription.img.numColumns];
			curDataRow = &frame[y * _pmdDataDescription.img.numColumns];
			for(int x = 0; x < _pmdDataDescription.img.numColumns; x++)
			{
				if(curDataRow[x] == PMD_INVALID_DISTANCE) continue;
				int avI = y * _pmdDataDescription.img.numColumns + x;
				if(framesForAverage[avI] > 0)
					curStdevRow[x] += pow(curDataRow[x] - curMeanRow[x], 2) / framesForAverage[avI];
				else
					curStdevRow[x] = PMD_INVALID_DISTANCE;
			}
		}
	}
	for (int i = 0; i < PMDIMAGESIZE; i++)
	{
		if(_background.stdevs[i] > 0)
			_background.stdevs[i] = sqrt(_background.stdevs[i]);
	}
	// show the mean  and stdev
	showBackgroundImage();

	// free all the frames
	for (int i = 0; i < _numFramesForBackgroundSubtraction; i++)
	{
		delete frames[i];
	}
}

void performBackgroundSubtraction()
{
	// in the depthbuffer, make anything that is within stdev of mean -1

	for (int i = 0; i < PMDIMAGESIZE; i++)
	{
		if(_pmdDistanceBuffer[i] == PMD_INVALID_DISTANCE) continue;
		if(_background.means[i] == PMD_INVALID_DISTANCE) continue;
		if(abs(_pmdDistanceBuffer[i]  - _background.means[i]) < BACKGROUND_THRESHOLD_STDEV * _background.stdevs[i])
		{
				_pmdDistanceBuffer[i] = PMD_INVALID_DISTANCE;
		}
	}
	
}

void medianFilter()
{
	// todo: store IplImage as global?
	IplImage* toFilter = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), IPL_DEPTH_32F, 1);
	IplImage* result = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), IPL_DEPTH_32F, 1);
	memcpy(toFilter->imageData, _pmdDistanceBuffer, PMDIMAGESIZE * sizeof(float));
	toFilter->imageDataOrigin = toFilter->imageData;
	cv::Mat src = toFilter;
	cv::Mat dst = result;

	medianBlur(src, dst, 5);
	memcpy(_pmdDistanceBuffer, result->imageData, PMDIMAGESIZE* sizeof(float));
	cvReleaseImage(&result);
	cvReleaseImage(&toFilter);

}

void showBackgroundImage()
{
	// todo: convert to a method (repeated once already)
	IplImage* toShow = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), 8, 3);
	depthDataToImage(_background.means, (unsigned char *)toShow->imageData, toShow->widthStep / sizeof(unsigned char), toShow->nChannels);

	cvFlip (toShow, toShow, 0);

    // Display the image
    cvShowImage ("Background Image", toShow);
	cvWaitKey(1);

	 cvReleaseImage(&toShow);

	//// show standard deviation
	toShow = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), 8, 3);
	depthDataToImage(_background.stdevs, (unsigned char *)toShow->imageData, toShow->widthStep / sizeof(unsigned char), toShow->nChannels);

	cvFlip (toShow, toShow, 0);
	cvShowImage ("Stdev Image", toShow);
	cvWaitKey(1);
	cvReleaseImage(&toShow);
}

void findFingers()
{
	// find the x, y coordinate that has the highest value
	_pmdData.fingerX = 0;
	_pmdData.fingerY = 0;
	_pmdData.fingerZ = FLT_MAX;

	for(int y = 0; y < _pmdDataDescription.img.numRows; y++)
	{
		for(int x = 0; x < _pmdDataDescription.img.numColumns; x++)
		{
			int idx = y * _pmdDataDescription.img.numColumns + x;
			if(_pmdDistanceBuffer[idx] >= 0 && _pmdDistanceBuffer[idx] < _pmdData.fingerZ)
			{
				_pmdData.fingerX = x;
				_pmdData.fingerY = y;
				_pmdData.fingerZ = _pmdDistanceBuffer[idx];
			}
		}
	}

	_fingerData.fingerX = _pmdData.fingerX;
	_fingerData.fingerY = _pmdData.fingerY;
	_fingerData.fingerZ = _pmdData.fingerZ;
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
		cout << "Could not get data description:" << _pmdErrorBuffer << endl;
		pmdClose (_pmdHandle);
		return -1;
	}

	if (_pmdDataDescription.subHeaderType != PMD_IMAGE_DATA)
	{
		fprintf (stderr, "Source data is not an image!\n");
		pmdClose (_pmdHandle);
		return -1;
	}

	// make sure that the numrows and numcolumsn is same size as PMDIMAGESIZE
	assert(_pmdDataDescription.img.numColumns * _pmdDataDescription.img.numRows == PMDIMAGESIZE);

	// todo: this shoudl be copied to a seperate location...
	res = pmdGetDistances (_pmdHandle, _pmdDistanceBuffer, _pmdDataDescription.img.numColumns * _pmdDataDescription.img.numRows * sizeof (float));
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
	for(int y = 0; y < _pmdDataDescription.img.numRows; y++)
	{
		for(int x = 0; x < _pmdDataDescription.img.numColumns; x++)
		{
			int idx = y * _pmdDataDescription.img.numColumns + x;
			if(_pmdFlags[idx] & PMD_FLAG_INVALID)
			{
				_pmdDistanceBuffer[idx] = PMD_INVALID_DISTANCE;
			}
		}
	}
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
	
	cout << "client first message: " << _fromClient.buffer << endl;

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

#ifdef NETWORK_DEBUG
		cout << "received: " << (_fromClient.buffer) << endl;
#endif

		char command = _fromClient.buffer[0];

		if(command == 'q') return false;
		if(command == 'd') return true;


		// fill the current send data with data from the camera
		// send the data
		memset(&_pmdData, 0, sizeof(PMDData));
		hr = updateCameraData();

		if(!SUCCEEDED(hr)) return true;
		threshold(_pmdDistanceBuffer, 0.1f);
		performBackgroundSubtraction();
		medianFilter();
		findFingers();

		if(command == 'f')
		{
			// only send the finger data
			hr = sendData(*hClient, (char*)&_fingerData, sizeof(PMDFingerData), 0);	
		} else
		{
			memcpy_s(_pmdData.buffer,_countof(_pmdData.buffer) * sizeof(float), _pmdDistanceBuffer, _countof(_pmdData.buffer) * sizeof(float));
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

	// Initialize the PMD camera
	// check if we have parameters, if so first param is filename
	if(argc > 1)
	{
		char* filename = argv[1];
		cout << "Reading data from file " << filename << endl;
		hr = initializePMDFromFile(&_pmdHandle,filename, _pmdErrorBuffer, BUFSIZE);
		if(!SUCCEEDED(hr)) error("Error: failed to initialize from file");
	} else 
	{
		hr = initializePMD(&_pmdHandle, _pmdErrorBuffer, BUFSIZE);
		if(!SUCCEEDED(hr)) error("Error: failed to initialize PMD camera");
	}

	initializeBackgroundSubtraction();
	
	WSADATA wsaData = {0};
	WORD wVer = MAKEWORD(2,2);

	// Step 1: initialize the socket
	hr = initWinsock(&wsaData, &wVer);
	if(!SUCCEEDED(hr)) return -1;

	cout << "Starting server on socket 10000..." << endl;

	SOCKET hSock  = {0};
	hSock = socket( AF_INET, SOCK_STREAM, IPPROTO_IP );
	if( hSock == INVALID_SOCKET ) { 
		error("Invalid socket, failed to create socket");
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
		if(!SUCCEEDED(hr)) error("Error: Failed to get client connection");

		stayAlive = communicateWithClient(&hClient);
		closesocket( hClient );
		hClient = 0;
	}

	cvReleaseImage (&_ocvFrame);

	cout << "Shutting down the server" << endl;

	// close server socket 
	hr = closesocket( hSock );
	hSock = 0;
	if( hr == SOCKET_ERROR )  cout << "Error: failed to close socket" << endl;


	// Release WinSock DLL 
	hr = WSACleanup();
	if( hr == SOCKET_ERROR ) cout << "Error: cleaning up Winsock Library" << endl;
}