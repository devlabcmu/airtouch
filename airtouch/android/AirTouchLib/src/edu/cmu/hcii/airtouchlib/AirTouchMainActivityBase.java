package edu.cmu.hcii.airtouchlib;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

public class AirTouchMainActivityBase extends Activity  implements ConnectTaskCompletedHandler{
	// Constants
		static final String TAG = "AirTouchViewMainActivityBase"; 
		// Instance variables

		// Network
		protected PMDServerConnection _connection;

		// UI
		EditText _ipEditText;
		EditText _portEditText;
		protected TextView _statusTextView;

		InputMethodManager _inputManager;

		protected AirTouchViewBase _airTouchView;
		protected boolean _canStart = false;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.airtouch_main_activity);


			_ipEditText = (EditText)findViewById(R.id.editTextIP);
			_portEditText = (EditText)findViewById(R.id.editTextPort);
			_statusTextView = (TextView)findViewById(R.id.textViewStatus);
			_portEditText.setText(Integer.toString(PMDServerConnection.DEFAULT_SERVER_PORT));
			_ipEditText.setText(PMDServerConnection.DEFAULT_IP_STRING);

			_inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

			_airTouchView = new AirTouchViewBase(this);
		}

		public void rootClicked(View v)
		{
			// unfocus text
			_inputManager.hideSoftInputFromWindow(_portEditText.getWindowToken(), 0);
		}

		public void disconnectClicked(View v)
		{
			_statusTextView.setText("Disconnected");
			// TODO actally disconnect here
		}

		@Override
		protected void onDestroy() {
			_airTouchView.stop();
			new DisconnectTask(_connection).execute();
			super.onDestroy();
		}

		@Override
		public void onBackPressed() 
		{
			finish();
		}
		public void connectClicked(View v)
		{
			_statusTextView.setText("Listening for connections on " + _ipEditText.getText().toString() + " port " + _portEditText.getText().toString() + "...");
			_connection = new PMDServerConnection(_ipEditText.getText().toString(), _portEditText.getText().toString() );
			new BindTask(this, _connection).execute();
		}

		public void viewFingerDataClicked(View v)
		{
			if(!_canStart){

				_statusTextView.setText("you must first connect!");
				return;
			}
			
			setContentView(_airTouchView);
		}
		
				@Override
		public void onConnectionCompleted(ConnectTaskResult result) {
			_canStart = result.success;
			_airTouchView.setServerConnection(_connection);
			if(result.success)
			{
				_statusTextView.setText("Connection successful!");
				Log.i(TAG, "Handshake successful!");
			} else
			{
				_statusTextView.setText("Connection failed :(");
				Log.i(TAG, "Failed to connect to server");
			}
			
		}
}
