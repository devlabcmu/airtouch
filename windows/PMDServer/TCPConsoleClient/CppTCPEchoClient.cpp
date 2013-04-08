// CppTCPEchoClient.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"

#include "simplewinsock.h"

using namespace std;


int main(int argc, const char* argv[])
{
	HRESULT hr = 0;
	WSADATA wsaData = {0};
	WORD wVer = MAKEWORD(2,2);

	// Step 1: initialize the socket
	hr = initWinsock(&wsaData, &wVer);
	if(!SUCCEEDED(hr)) return -1;

	cout << "Opening connection to server" << endl;
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
	saServer.sin_addr.s_addr = inet_addr( "127.0.0.1" );
	
	// connect 
	hr = connectSocket( &hServer, &saServer );  
	if(!SUCCEEDED(hr)) return -1;

	cout << "Connected to server" << endl;
	cout << "Sending data to server" << endl;

	// process data 
	bool alive = true;
	char wzRec[1024] = {0};
	while(alive){
		string inputstr;
		getline(cin, inputstr);
		sendData(hServer, inputstr.c_str(),inputstr.length(), 0);
		receiveData(hServer, wzRec, 1024, false);
		cout << "Data received: " << wzRec << endl;
	}



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

