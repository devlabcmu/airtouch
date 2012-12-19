#include <stdio.h>
#include <tchar.h>



#include "simplewinsock.h"
#include "strutils.h"
#include"pmddata.h"

#include <opencv/cxcore.h>
#include <opencv/highgui.h>

#define BUFSIZE 512

PMDSendData _fromServer;
PMDReceiveData _toServer;
char _handShake[BUFSIZE];

// OpenCV
IplImage* _ocvFrame;
int _ocvFrameStep;

// render the received data in opencv frame
// render the finger in opencv frame

void updateFrameWithNewData()
{
	unsigned char * currentRow = 0;
    unsigned char * imgPtr = (unsigned char *) _ocvFrame->imageData;
	float * dataPtr = _fromServer.buffer;
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
	cvCircle(_ocvFrame, cvPoint(cvRound(_fromServer.fingerX), 
		PMDNUMROWS - cvRound(_fromServer.fingerY)), 
		10, 
		CV_RGB(255,0,0));
    // Display the image
    cvShowImage ("Live image", _ocvFrame);
}

int main(int argc, char* argv[])
{
	HRESULT hr = 0;
	WSADATA wsaData = {0};
	WORD wVer = MAKEWORD(2,2);

	// initialize opencv frame
	_ocvFrame = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), 8, 3);
	_ocvFrameStep = _ocvFrame->widthStep / sizeof(unsigned char);

	// Step 1: initialize the socket
	hr = initWinsock(&wsaData, &wVer);
	if(!SUCCEEDED(hr)) return -1;

	char* ip = "127.0.0.1";
	if(argc > 2) ip = argv[1];
	cout << "ip is " << ip << endl;

	cout << "Opening connection to " << ip << " at port 10000" << endl;
	SOCKET hServer  = {0};
	// open a socket 
	// 
	// for the server we do not want to specify a network address 
	// we should always use INADDR_ANY to allow the protocal stack 
	// to assign a local IP address 
	hServer = socket( AF_INET, SOCK_STREAM, IPPROTO_IP );
	if( hServer == INVALID_SOCKET ) { 
		cout << "Invalid socket, failed to create socket" << endl;
		return -1;
	} 
	// name a socket 
	sockaddr_in saServer = {0};
	saServer.sin_family      = PF_INET;    
	saServer.sin_port        = htons( 10000 );       
	saServer.sin_addr.s_addr = inet_addr( ip );
	
	// connect 
	hr = connectSocket( &hServer, &saServer );  
	if(!SUCCEEDED(hr)) return -1;

	cout << "Connected to server" << endl;
	
	// send device info
	memset(_handShake, 0, PMDBUFSIZE);
	memset(_toServer.buffer, 0, sizeof(PMDReceiveData));
	strcpy_s(_handShake, PMDBUFSIZE, "device info\n");

	sendData(hServer, _handShake,strlen(_handShake), 0);
	receiveData(hServer, _handShake, BUFSIZE, 0);
	cout << "server reply: " << _handShake;

	// process data 
	strcpy_s(_toServer.buffer,BUFSIZE, "gimme");
	while(true){
		// send 'gimme'
		hr = sendData(hServer, _toServer.buffer, strlen(_toServer.buffer), 0);
		if(!SUCCEEDED(hr)) break;
		// parse and display data
		int numBytesReceived = receiveData(hServer, (char*)&_fromServer, sizeof(PMDSendData), 0);
		if(numBytesReceived <= 0) break;
		int c = cvWaitKey (1);
		if (c == 'q' || c == 'Q' || c == 27) break;
        
		// display the data in an opencv window
		updateFrameWithNewData();
	}

	cvReleaseImage (&_ocvFrame);

	cout << "Closing connection" << endl;
	// shutdown socket 
	hr = shutdown( hServer, SD_BOTH );
	if( hr == SOCKET_ERROR ) { 
		// WSAGetLastError() 
		cout << "Error trying to perform shutdown on socket" << endl;
		return -1;
	} 
	// close server socket 
	hr = closesocket( hServer );
	hServer = 0;
	if( hr == SOCKET_ERROR ) { 
		cout << "Error failed to close socket" << endl;
	} 

	// Release WinSock DLL 
	hr = WSACleanup();
	if( hr == SOCKET_ERROR ) { 
		cout << "Error cleaning up Winsock Library" << endl;
		return -1;
	} 
	cout << "Data sent successfully" << endl;
	return 0;
}