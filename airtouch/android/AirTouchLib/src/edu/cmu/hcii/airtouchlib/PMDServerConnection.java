package edu.cmu.hcii.airtouchlib;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.Log;

public class PMDServerConnection
{
	public static final int DEFAULT_SERVER_PORT = 10000;
	public static final String DEFAULT_IP_STRING = "127.0.0.1";
	public static final int MAX_TCP_DATAGRAM_LEN = 1024;
	
	public  InetAddress _serverAddr;
	public  int _serverPort;
	public Socket _clientSocket;
	public ServerSocket _serverSocket;
	public InputStream _inFromServer;
	public DataOutputStream _outToServer;
	
	public PMDServerConnection(String ipStr, String socketStr)
	{
		try {
			_serverAddr = InetAddress.getByName(ipStr);
		} catch (UnknownHostException e) {
			Log.e("PMDServerConnection", e.getMessage());
		}
		_serverPort = Integer.parseInt(socketStr);
		_clientSocket = new Socket();
		try {
			_serverSocket = new ServerSocket();
		} catch (IOException e) {
			Log.e("PMDServerConnection", "Failed to make a server socket");
		}
	}
	
}