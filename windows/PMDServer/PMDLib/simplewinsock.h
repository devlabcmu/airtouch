#pragma once

#include <stdio.h>
#include <tchar.h>

#include <WinSock2.h>
#include <iostream>
#include <process.h>
#include <Windows.h>
#include <sstream>

using namespace std;

#define DEFAULT_BUFLEN 512 // Not currently used


// information on how to use sockets for windows can be found at
// http://msdn.microsoft.com/en-us/library/windows/desktop/ms738566(v=vs.85).aspx

// Initialize WinSock2.2 DLL 
// low word = major, highword = minor
HRESULT initWinsock(WSADATA* wsaData, WORD* wVer)
{
	int nRet = WSAStartup( *wVer, wsaData );
	if( nRet == SOCKET_ERROR ) { 
		cout << "Failed to init Winsock library" << endl;
		return -1;
	} 
	return 0;
}

HRESULT bindSocket(SOCKET* hSocket, sockaddr_in* socketInfo)
{
	HRESULT hr = bind( *hSocket, (sockaddr*)socketInfo, sizeof(sockaddr) );
	if( hr == SOCKET_ERROR ) { 
		cout << "Failed to bind socket" << endl;
		//shutdown( hSock ); 
		closesocket( *hSocket );
		return -1;
	} 
	return 0;
}

HRESULT connectSocket(SOCKET* hSocket, sockaddr_in* socketInfo)
{
	HRESULT hr = connect( *hSocket, (sockaddr*)socketInfo, sizeof( sockaddr ) );  
	if( hr == SOCKET_ERROR ) {
		
		cout << "Connection to server failed err no:" << GetLastError()  << endl;
		closesocket( *hSocket );
		return -1;
	} 
	return 0;
}

HRESULT getClientConnection(SOCKET* hSock, SOCKET* hClient)
{
		HRESULT hr = listen( *hSock, 5 ); // connection backlog queue set to 10 
		if( hr == SOCKET_ERROR ) 
		{ 
			int nErr = WSAGetLastError();
			if( nErr == WSAECONNREFUSED ) { 
				cout << "Failed to listen, connection refused" << endl;
			} 
			else { 
				cout << "Call to listen failed: " << nErr << endl;
			} 
			closesocket( *hSock );
			return -1;
		} 
		// connect 
		sockaddr_in saClient = {0};
		int nSALen = sizeof( sockaddr );
		*hClient = accept( *hSock, (sockaddr*)&saClient, &nSALen );
		if( *hClient == INVALID_SOCKET ) { 
			cout << "Invalid client socket, connection failed" << endl;
			closesocket( *hClient );
			return -1;
		} 
		cout << "Connection estabilished" << endl;
		return 0;
}

// Send data on a socket
// data: data to send
// len: length of data
// flags: any flags to use.
int sendData(SOCKET socket, const char* data, int len, int flags)
{
	int iPos  = 0;
	int nLeft = len;
	int nData = 0;
	do
	{ 
		nData = send( socket, &data[iPos], nLeft, 0 );
		if( nData == SOCKET_ERROR ) { 
			cout << "Error sending data" << endl;
			return -1;
		} 
		nLeft -= nData;
		iPos  += nData;
	} while( nLeft > 0 );
	return 0;
}

// Receive data on a socket. Returns number of bits filled, or -1 if error
// buffer: buffer to fill
// len: maximum length of the buffer
// mustFill: whether the buffer must be filled

int receiveData(SOCKET socket, char* buffer, int len, bool mustFill)
{
	int nLeft  = len;
	int iPos   = 0;
	int nData  = 0;

	// clear data buffer 
	memset( buffer, 0, len );
	do
	{ 
		nData = recv( socket, &buffer[iPos], nLeft, 0 );
		if( nData == SOCKET_ERROR ) { 
			cout << "Error receiving data" << endl;
			return -1;
		} 
		if(!mustFill)
			return nData;
		
		nLeft -= nData;
		iPos  += nData;
	} while( nLeft > 0 );
	return len - nLeft;
}