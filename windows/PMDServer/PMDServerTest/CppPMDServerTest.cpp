#include <stdio.h>
#include <tchar.h>

#include "simplewinsock.h"
#include "strutils.h"
#include"pmddata.h"
#include "PMDCamera.h"
#include "PMDUtils.h"

#include <opencv/cxcore.h>
#include <opencv/highgui.h>

#define BUFSIZE 512
#define BIT_DEBUG false

PMDData _fromServer;
PMDRequest _toServer;
char _handShake[BUFSIZE];

// OpenCV
IplImage* _ocvFrame;
int _ocvFrameStep;

// render the received data in opencv frame
// render the finger in opencv frame

void updateFrameWithNewData()
{
	PMDUtils::DistancesToImage(_fromServer.buffer, _ocvFrame);

    // Display the image
    cvShowImage ("Client", _ocvFrame);
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
	memset(_handShake, 0, PMDREQUESTSIZE);
	memset(_toServer.buffer, 0, sizeof(PMDRequest));
	strcpy_s(_handShake, PMDREQUESTSIZE, "device info\n");

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
		int numBytesReceived = receiveData(hServer, (char*)&_fromServer, sizeof(PMDData), true);
		if(numBytesReceived <= 0) break;
		int c = cvWaitKey (1);
		if (c == 'q' || c == 'Q' || c == 27) break;

		for (int i = 0; i < 2; i++)
		{
			cout << _fromServer.fingers[i].id << ":" << "(" << _fromServer.fingers[i].x << ", " << _fromServer.fingers[i].y << ", " << _fromServer.fingers[i].z << ")" << endl;
		}
		
		
		if(BIT_DEBUG){
			byte* tmp = (byte*)&_fromServer;
		
			for (int i = 0; i < 3; i++)
			{
				ostringstream oss;
				for(int j = 0; j < 4; j++, tmp++)
				{
					byte b = *tmp;
					for (int k = 0; k < 8; k++)
					{
						unsigned int n = (b & (1 << k)) >> k;
						oss << n;
					}
					oss << " " ;
				}	
				string outs = oss.str();
				reverse(outs.begin(), outs.end());
				cout << "i: " << i << ", bits:" << outs << endl;
			}
		}
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