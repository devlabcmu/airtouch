// CppTCPEcho.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include "simplewinsock.h"
#include "strutils.h"

using namespace std;

void welcomeMessage()
{
	cout << "I echo what the client sends me!" << endl;
	cout << "--------------------------------" << endl;
	cout << "To disconnect, send !disconnect" << endl;
	cout << "To shutdown, send !shutdown" << endl;
}

// Waits for a client to connect,
// then echos what client says until the client sends !disconnect
// if client sends !shutdown, server shuts down
int _tmain(int argc, _TCHAR* argv[])
{
	welcomeMessage();

	HRESULT hr = 0;
	WSADATA wsaData = {0};
	WORD wVer = MAKEWORD(2,2);

	// Step 1: initialize the socket
	hr = initWinsock(&wsaData, &wVer);
	if(!SUCCEEDED(hr)) return -1;

	cout << "Starting server" << endl;

	// step 2: create socket
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

	bool stayAlive = true;
	bool stayConnected = true;
	while( stayAlive ) 
	{ 
		cout << "Listening for connections" << endl;

		// step 4: listen for a client 
		SOCKET hClient;
		hr = getClientConnection(&hSock, &hClient);
		if(!SUCCEEDED(hr)) return -1;

		while(stayConnected)
		{
			// receive data data 
			char wzRec[512] = {0};		
			int result = receiveData(hClient, wzRec, 512, false);

			// check input data
			if(!SUCCEEDED(result))
			{
				stayConnected = false;
				continue;
			}
			
			if( _stricmp( trim(string(wzRec)).c_str(), "!shutdown" ) == 0 ) 
			{
				cout << "Ahh! shutdown command received" << endl;
				stayAlive = false;
				stayConnected = false;
				continue;
			} 
			if(_stricmp(trim(string(wzRec)).c_str(),"!disconnect") == 0)
			{
				cout << "Ahh! disconnect command received" << endl;
				stayConnected = false;
				continue;
			}

			cout << "Data Received: " << wzRec << endl;

			// echo data back to client 
			sendData(hClient, wzRec, 512, 0);
			
			
		}

		// close client connection 
		closesocket( hClient );
		hClient = 0;
		// perform a lowercase comparison

	} // loop 
	cout << "Shutting down the server" << endl;

	// close server socket 
	hr = closesocket( hSock );
	hSock = 0;
	if( hr == SOCKET_ERROR )  cout << "Error failed to close socket" << endl;


	// Release WinSock DLL 
	hr = WSACleanup();
	if( hr == SOCKET_ERROR ) cout << "Error cleaning up Winsock Library" << endl;
	
	return 0;
}



