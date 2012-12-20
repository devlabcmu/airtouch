package edu.cmu.hcii.tcpcommunicator;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

public class TCPCommunicatorMain extends Activity {

	// Constants
	static final int DEFAULT_SERVER_PORT = 10000;
	static final String DEFAULT_IP_STRING = "128.237.118.131";
	static final int MAX_TCP_DATAGRAM_LEN = 1024;
	static final String TAG = "TCPCommunicator";

	// Instance variables

	// Network related
	InetAddress _serverAddr;
	Socket _clientSocket;
	DataOutputStream _outToServer;
	BufferedReader _inFromServer;
	int _serverPort;

	// UI elements
	EditText _ipEditText;
	EditText _portEditText;
	EditText _sendText;
	TextView _statusTextView;
	TextView _receiveTextView;
	InputMethodManager _inputManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tcpcommunicator_main);

		_ipEditText = (EditText) findViewById(R.id.editTextIP);
		_portEditText = (EditText) findViewById(R.id.editTextPort);
		_sendText = (EditText) findViewById(R.id.editTextSendMsg);
		_statusTextView = (TextView) findViewById(R.id.textViewStatus);
		_receiveTextView = (TextView) findViewById(R.id.textViewReceive);

		_portEditText.setText(Integer.toString(DEFAULT_SERVER_PORT));
		_ipEditText.setText(DEFAULT_IP_STRING);

		_inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_tcpcommunicator_main, menu);
		return true;
	}

	public void connectClicked(View v) {
		// todo: verify that the ip string is valid
		String hostStr = _ipEditText.getText().toString();
		String portStr = _portEditText.getText().toString();
		_serverPort = Integer.parseInt(portStr);
		new ConnectTask().execute(hostStr);

		
	}

	public void sendClicked(View v) {
		new SendReceiveTask().execute(_sendText.getText().toString());
	}

	public void rootClicked(View v) {
		// unfocus text
		_inputManager
				.hideSoftInputFromWindow(_portEditText.getWindowToken(), 0);
	}

	public void disconnectClicked(View v) {
		if (_clientSocket == null)
			return;
		try {
			_clientSocket.close();
		} catch (IOException e) {
			Log.v(TAG, e.getMessage());
		}
		_receiveTextView.setText("Received data goes here");
		_statusTextView.setText("Disconnected");
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
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
				
				
				Log.v(TAG, "getting input stream...");
				_inFromServer = new BufferedReader(new InputStreamReader(
						_clientSocket.getInputStream()));
				Log.v(TAG, "getting output stream...");
				_outToServer = new DataOutputStream(_clientSocket.getOutputStream());
				
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
				_statusTextView.setText("Connected");
			} else {
				_statusTextView.setText("Connection failed, see logcat.");
			}
		}
		
	}
	
	class SendReceiveTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			if (_clientSocket == null) {
				Log.v(TAG, "Error: Tried to send string but socket was null");
				return null;

			}
			if (_clientSocket.isClosed()) {
				Log.v(TAG, "Error: Tried to send string but socket was closed");
				return null;
			}
			StringBuilder sb = new StringBuilder();
			for (String s : params) {
				try {
					// first send the data to the server
					_outToServer.writeBytes(s);

					// then read the reply from the server
					char[] lMsg = new char[MAX_TCP_DATAGRAM_LEN];
					int nReceived = _inFromServer.read(lMsg);
					sb.append(new String(lMsg, 0, nReceived));
				} catch (IOException e) {
					Log.v(TAG, "error sending string: " + e.getMessage());
					return null;
				}
			}
			return sb.toString();
		}

		@Override
		protected void onPostExecute(String result) {
			// update editText
			if (result != null) {
				_receiveTextView.setText(result
						+ _receiveTextView.getText().toString());
			}
		}
	}

}
