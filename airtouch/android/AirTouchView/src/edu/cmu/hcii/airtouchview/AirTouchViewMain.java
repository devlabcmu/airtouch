package edu.cmu.hcii.airtouchview;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.LauncherActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;


public class AirTouchViewMain extends Activity {
	// Constants
		static final int DEFAULT_SERVER_PORT = 11111;
		static final String DEFAULT_IP_STRING = "128.237.118.131";
		static final int MAX_UDP_DATAGRAM_LEN = 1500;
		static final String TAG = "UDPCommunicator"; 
		
		// Instance variables
		InetAddress _serverAddr;
		EditText _ipEditText;
		EditText _portEditText;
		EditText _sendText;
		TextView _statusTextView;
		TextView _receiveTextView;
		DatagramSocket _socket;
		InputMethodManager _inputManager;

		AirTouchView _airTouchView;
		int _serverPort;
		    
		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_airtouchview_main);
	        
	        _ipEditText = (EditText)findViewById(R.id.editTextIP);
	        _portEditText = (EditText)findViewById(R.id.editTextPort);
	        _sendText = (EditText)findViewById(R.id.editTextSendMsg);
	        _statusTextView = (TextView)findViewById(R.id.textViewStatus);
	        _receiveTextView = (TextView)findViewById(R.id.textViewReceive);
	        
	        
	        _portEditText.setText(Integer.toString(DEFAULT_SERVER_PORT));
	        _ipEditText.setText(DEFAULT_IP_STRING);
	        
	        _inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

	        _airTouchView = new AirTouchView(this);
	    }
	    
	    public void connectClicked(View v)
	    {
	    	// todo: verify that the ip string is valid
	    	String ipStr = _ipEditText.getText().toString();
	    	String portStr = _portEditText.getText().toString();

	    	_serverPort = Integer.parseInt(portStr);
	    	
	    	try {
	    		_socket = new DatagramSocket();
	    		
				_serverAddr = InetAddress.getByName(ipStr);
			} catch (UnknownHostException e) {

				Log.v(TAG, e.getMessage());
				return;
			} catch (SocketException e)
			{
				Log.v(TAG, e.getMessage());
				return;
			} 
	    	
	    	_airTouchView.initializeConnection(_socket, _serverPort, _serverAddr);
	    	
	    	Log.v(TAG, "connection established to port " + portStr + " and ip " + ipStr);
	    	new ReceiveStringTask().execute();
	    	_statusTextView.setText("Connected");
	    }
	    
	    public void sendClicked(View v)
	    {
	    	new SendStringTask().execute(_sendText.getText().toString());
	    }
	    
	    public void rootClicked(View v)
	    {
	    	// unfocus text
	    	_inputManager.hideSoftInputFromWindow(_portEditText.getWindowToken(), 0);
	    }
	    
	    public void disconnectClicked(View v)
	    {
	    	if(_socket == null) return;
	    	_socket.close();
	    	_receiveTextView.setText("Received data goes here");
	    	_statusTextView.setText("Disconnected");
	    }

	    @Override
	    public void onBackPressed() 
	    {
	    	super.onBackPressed();
	    }
	    
	    public void viewRawClicked(View v)
	    {
	    	setContentView(_airTouchView);
	    }
	    
	    class ReceiveStringTask extends AsyncTask<Void, Void, String>
	    {
	    	@Override
	    	protected void onPreExecute() {
	    		super.onPreExecute();
	    		Log.v(TAG, "receiving on port " + _serverPort + "...");
	    	}
			@Override
			protected String doInBackground(Void... params) {
				// TODO Auto-generated method stub
				if(_socket == null) return null;
				if(_socket.isClosed()) return null;
				String result;
				byte[] lMsg = new byte[MAX_UDP_DATAGRAM_LEN];
				DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length);
				try {
					_socket.setSoTimeout(1000);
					_socket.receive(dp);
				}catch (InterruptedIOException e)
				{
//					Log.v(TAG,"receive timeout, restarting...");
					return null;
				}catch (IOException e) {
//					Log.v(TAG, "error receiving data " + e.getMessage());
					return null;
				} 
				result = new String(lMsg, 0, dp.getLength());
				Log.v(TAG, "UDP packet received " + result);
				return result;
			}
			@Override
			protected void onPostExecute(String result) {
				// update editText
				if(result != null)
				{
					_receiveTextView.setText(result + _receiveTextView.getText().toString());
				}
				if(_socket != null)
				{
					new ReceiveStringTask().execute();
				}
			}
	    }
	    
	    class SendStringTask extends AsyncTask<String, Void, Boolean>
	    {

			@Override
			protected Boolean doInBackground(String... params) {
		    	if(_socket == null)
		    	{
		    		Log.v(TAG, "Error: Tried to send string but socket was null");
		    		return false;

		    	}
		    	for (String s : params) 
		    	{
					DatagramPacket dp;
					dp = new DatagramPacket(s.getBytes(), s.length(), _serverAddr, _serverPort);
					try {
						_socket.send(dp);
					} catch (IOException e) {
						Log.v(TAG, "error sending string: " + e.getMessage());
						return false;
					}				
				}

				return true;
			}
	    }

}