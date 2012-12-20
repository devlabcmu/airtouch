#include <stdio.h>
#include <tchar.h>
#include <assert.h>

#include "simplewinsock.h"
#include "strutils.h"
#include"simplepmd.h"

using namespace std;

#define BUFSIZE 512

PMDSendData _sendData;
PMDReceiveData _receiveData;

PMDDataDescription _pmdDataDescription;
PMDHandle _pmdHandle;
unsigned int _pmdFlags[PMDIMAGESIZE];
char _pmdErrorBuffer[BUFSIZE];

/* function declarations */
// PMD
HRESULT get3DData();

// UI
void welcomeMessage();

// Networking
void updateSendData();
bool communicateWithClient(SOCKET* hClient);

void welcomeMessage()
{
	// add welcome here
	cout << "PMDServer sends data from pmd camera or a file" << endl;
	cout << "Usage: PMDServer.exe to read from camera" << endl;
	cout << "Usage: PMDServer.exe [filename.pmd] to read from .pmd file" << endl;
}

//
// PMD
//

// initializes the PMD sensor
// returns error code


HRESULT get3DData()
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

	res = pmdGetDistances (_pmdHandle, _sendData.buffer, _pmdDataDescription.img.numColumns * _pmdDataDescription.img.numRows * sizeof (float));
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
	_sendData.fingerX = 0;
	_sendData.fingerY = 0;
	_sendData.fingerZ = FLT_MAX;

	for(int y = 0; y < _pmdDataDescription.img.numRows; y++)
	{
		for(int x = 0; x < _pmdDataDescription.img.numColumns; x++)
		{
			int idx = y * _pmdDataDescription.img.numColumns + x;
			if(_pmdFlags[idx] & PMD_FLAG_INVALID)
			{
				_sendData.buffer[idx] = 1;
			} else if(_sendData.buffer[idx] < _sendData.fingerZ)
			{
				_sendData.fingerX = x;
				_sendData.fingerY = y;
				_sendData.fingerZ = _sendData.buffer[idx];
			}
		}
	}
	return 0;
}

//
// Networking 
//

void updateSendData()
{
	// fill the data with data from the camera

}

// Initializes connection to client and sends data.
// Returns when client disconnects
// Returns true if client sends a shutdown message.
bool communicateWithClient(SOCKET* hClient)
{
	bool result = true;
	// receive params about client 
	memset(_sendData.buffer, 0, BUFSIZE);
	memset(_receiveData.buffer, 0, BUFSIZE);

	cout << "receiving initial handshake..." << endl;

	int bytesReceived = receiveData(*hClient, _receiveData.buffer, 512, false);

	// check input data
	if(!SUCCEEDED(result))
	{
		cout << "failed to receive data from client, disconnecting..." << endl;
		return true;
	}
	
	cout << "client first message: " << _receiveData.buffer  << "echoing..." << endl;


	// send a reply, simply echo for now
	HRESULT hr = sendData(*hClient, _receiveData.buffer, BUFSIZE, 0);
	if(!SUCCEEDED(hr)) return true;

	while(true)
	{
		// receive data from client
		// check first int, if 'd' then disconnect
		// if 'q' then shutdown
		bytesReceived = receiveData(*hClient, _receiveData.buffer, 512, false);
		if(bytesReceived == 0) return true;
		// write the rest of the data send to the screen
		cout << "received: " << (_receiveData.buffer) << endl;

		char command = _receiveData.buffer[0];

		if(command == 'q') return false;
		if(command == 'd') return true;

		// fill the current send data with data from the camera
		// send the data
		memset(&_sendData, 0, sizeof(PMDSendData));
		hr = get3DData();
		if(!SUCCEEDED(hr)) return true;
		cout << "sending " << sizeof(PMDSendData) << " bytes" << endl;
		hr = sendData(*hClient, (char*)&_sendData, sizeof(PMDSendData), 0);	

		if(!SUCCEEDED(hr)) return true;

	}

	// close client connection 

	return result;
}

int main(int argc, char* argv[])
{
	// welcome message
	welcomeMessage();
	
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

	cout << "Shutting down the server" << endl;

	// close server socket 
	hr = closesocket( hSock );
	hSock = 0;
	if( hr == SOCKET_ERROR )  cout << "Error failed to close socket" << endl;


	// Release WinSock DLL 
	hr = WSACleanup();
	if( hr == SOCKET_ERROR ) cout << "Error cleaning up Winsock Library" << endl;
}