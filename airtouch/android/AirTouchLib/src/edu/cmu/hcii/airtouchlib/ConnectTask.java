package edu.cmu.hcii.airtouchlib;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

public class ConnectTask extends AsyncTask<String, Void, Boolean>
{
	static final String LOG_TAG = "AirTouchLib.ConnectTask";
	
	PMDServerConnection _connection;
	
	/**
	 * 
	 */
	private final ConnectTaskCompletedHandler _completedHandler;

	/**
	 * @param airTouchViewMain
	 */
	public ConnectTask(ConnectTaskCompletedHandler completedHandler, PMDServerConnection connection) {
		this._completedHandler = completedHandler;
		_connection = connection;
	}

	@Override
	protected void onPreExecute() {
	}

	@Override
	protected Boolean doInBackground(String... params) {
		try {
			
			_connection._clientSocket.connect(new InetSocketAddress(_connection._serverAddr, _connection._serverPort), 2000);
			// When just receiving small packets (ie. just finger lcation, no depth, you will want to setTcpNoDelay(true)
			// thiw will allow for immediate receiving of packets even when data sent is small
			_connection._clientSocket.setTcpNoDelay(true);

			Log.v(LOG_TAG, "getting input stream...");
			_connection._inFromServer = _connection._clientSocket.getInputStream();
			Log.v(LOG_TAG, "getting output stream...");
			_connection._outToServer = new DataOutputStream(_connection._clientSocket.getOutputStream());

			// do a handshake
			byte[] lMsg = new byte[PMDServerConnection.MAX_TCP_DATAGRAM_LEN];
			// send device info
			_connection._outToServer.writeBytes("device model: " + android.os.Build.MODEL + "\n");

			int nReceived = _connection._inFromServer.read(lMsg);
			Log.v(LOG_TAG, "from server: " + new String(lMsg, 0, nReceived));

			// TODO Fix this
//			this.airTouchViewMain._airTouchView.setupServerConnection(this.airTouchViewMain._inFromServer, this.airTouchViewMain._outToServer);
			// set the buffers for airtouchview

		}
		catch (UnknownHostException e) {
			Log.v(LOG_TAG, e.getMessage());
			return false;

		} 
		catch (IOException e) {
			Log.v(LOG_TAG, e.getMessage());
			return false;
		}

		return true;

	}

	@Override
	protected void onPostExecute(Boolean success) {
		ConnectTaskResult r = new ConnectTaskResult();
		r.success = success;
		Log.v("ConnectTask", "Connection completed");
		_completedHandler.onConnectionCompleted(r);
	}

}