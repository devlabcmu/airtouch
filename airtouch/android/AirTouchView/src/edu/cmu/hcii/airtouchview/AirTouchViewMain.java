package edu.cmu.hcii.airtouchview;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;


public class AirTouchViewMain extends Activity {
	// Constants
		static final int DEFAULT_SERVER_PORT = 10000;
		static final String DEFAULT_IP_STRING = "128.237.205.139";
		static final String TAG = "AirTouchView"; 
		static final int MAX_TCP_DATAGRAM_LEN = 1024;
		
		// Instance variables
		
		// Network
		InetAddress _serverAddr;		
		Socket _clientSocket;		
		int _serverPort;
		InputStream _inFromServer;
		DataOutputStream _outToServer;
	    
		// UI
		EditText _ipEditText;
		EditText _portEditText;
		TextView _statusTextView;


		InputMethodManager _inputManager;

		AirTouchView _airTouchView;
		boolean _canStart = false;

		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_airtouchview_main);
	        
	        _ipEditText = (EditText)findViewById(R.id.editTextIP);
	        _portEditText = (EditText)findViewById(R.id.editTextPort);
	        _statusTextView = (TextView)findViewById(R.id.textViewStatus);
	        _portEditText.setText(Integer.toString(DEFAULT_SERVER_PORT));
	        _ipEditText.setText(DEFAULT_IP_STRING);
	        
	        _inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

	        _airTouchView = new AirTouchView(this);
	    }
		
		public void rootClicked(View v)
	    {
	    	// unfocus text
	    	_inputManager.hideSoftInputFromWindow(_portEditText.getWindowToken(), 0);
	    }
	    
	    public void disconnectClicked(View v)
	    {
	    	if(_clientSocket == null) return;
	    	try {
				_clientSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	_statusTextView.setText("Disconnected");
	    }

	    @Override
	    protected void onDestroy() {
	    	_airTouchView.stop();
	    	super.onDestroy();
	    }
	    
	    
	    
	    @Override
	    public void onBackPressed() 
	    {
	    	super.onBackPressed();
	    }
	    public void connectClicked(View v)
	    {
	    	_serverPort = Integer.parseInt(_portEditText.getText().toString());
	    	new ConnectTask().execute(_ipEditText.getText().toString());
	    }
	    
	    public void viewRawClicked(View v)
	    {
	    	if(!_canStart){
	    		
	    		_statusTextView.setText("you must first connect!");
	    		return;
	    	}
	    	setContentView(_airTouchView);
	    }
	    
		class ConnectTask extends AsyncTask<String, Void, Boolean>
		{
			@Override
			protected void onPreExecute() {
				_statusTextView.setText("Connecting...");
			}
			
			@Override
			protected Boolean doInBackground(String... params) {
				try {
					_serverAddr = InetAddress.getByName(params[0]);
					_clientSocket = new Socket();
					_clientSocket.connect(new InetSocketAddress(_serverAddr, _serverPort), 2000);
					// When just receiving small packets (ie. just finger lcation, no depth, you will want to setTcpNoDelay(true)
					// thiw will allow for immediate receiving of packets even when data sent is small
					_clientSocket.setTcpNoDelay(true);
					
					Log.v(TAG, "getting input stream...");
					_inFromServer = _clientSocket.getInputStream();
					Log.v(TAG, "getting output stream...");
					_outToServer = new DataOutputStream(_clientSocket.getOutputStream());
					
					// do a handshake
					byte[] lMsg = new byte[MAX_TCP_DATAGRAM_LEN];
					// send device info
					_outToServer.writeBytes("device model: " + android.os.Build.MODEL + "\n");
					
					int nReceived = _inFromServer.read(lMsg);
					Log.v(TAG, "from server: " + new String(lMsg, 0, nReceived));
					
					_airTouchView.setupServerConnection(_inFromServer, _outToServer);
					// set the buffers for airtouchview
					
				}
				catch (UnknownHostException e) {
					Log.v(TAG, e.getMessage());
					return false;
					
				} 
				catch (IOException e) {
					Log.v(TAG, e.getMessage());
					return false;
				} catch (Exception e) {
					Log.v(TAG, e.toString());
					return false;
				}

				return true;

			}
			
			@Override
			protected void onPostExecute(Boolean success) {
				if (success) {
					_canStart = true;
					_statusTextView.setText("Connected, handshake complete. You can now press start.");
				} else {
					_statusTextView.setText("Connection failed, see logcat.");
				}
			}
			
		}
	
}
