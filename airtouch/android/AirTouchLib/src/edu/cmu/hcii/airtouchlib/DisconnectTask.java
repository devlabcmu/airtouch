package edu.cmu.hcii.airtouchlib;

import java.io.IOException;

import android.os.AsyncTask;
import android.util.Log;

public class DisconnectTask extends AsyncTask<Void, Void, Void>
{
	static final String TAG="DisconnectTask";
	private final PMDServerConnection _server;

	public DisconnectTask(PMDServerConnection server) {
		this._server = server;
	}

	@Override
	protected Void doInBackground(Void... params) {
		try {
			if(_server != null && _server._outToServer != null)
			{
				this._server._outToServer.writeBytes("disconnect");
				if(_server._serverSocket != null)
					this._server._serverSocket.close();
			}
		} catch (IOException e) {
			Log.i(TAG, "Exception when disconnecting: " + e.getMessage());
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		// TODO: update the UI here
//		_errorText  = "Error: disconnected from server due to an error";
//		postInvalidate();
	}
}