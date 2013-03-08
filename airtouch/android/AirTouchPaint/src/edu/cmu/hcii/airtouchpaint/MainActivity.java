package edu.cmu.hcii.airtouchpaint;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import edu.cmu.hcii.airtouchlib.BindTask;
import edu.cmu.hcii.airtouchlib.ConnectTaskCompletedHandler;
import edu.cmu.hcii.airtouchlib.ConnectTaskResult;
import edu.cmu.hcii.airtouchlib.PMDServerConnection;

public class MainActivity extends Activity implements ConnectTaskCompletedHandler  {
	// Constants
		static final String TAG = "AirTouchView"; 
		// Instance variables

		// Network
		PMDServerConnection _connection;

		// UI
		EditText _ipEditText;
		EditText _portEditText;
		TextView _statusTextView;


		InputMethodManager _inputManager;

		AirTouchPaintView _view;
		boolean _canStart = false;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);

			_ipEditText = (EditText)findViewById(R.id.editTextIP);
			_portEditText = (EditText)findViewById(R.id.editTextPort);
			_statusTextView = (TextView)findViewById(R.id.textViewStatus);
			_portEditText.setText(Integer.toString(PMDServerConnection.DEFAULT_SERVER_PORT));
			_ipEditText.setText(PMDServerConnection.DEFAULT_IP_STRING);

			_inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

			_view = new AirTouchPaintView(this);
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
			_view.stop();
			super.onDestroy();
		}

		@Override
		public void onBackPressed() 
		{
			super.onBackPressed();
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
			setContentView(_view);
		}

		public void viewRawClicked(View v)
		{
			if(!_canStart){

				_statusTextView.setText("you must first connect!");
				return;
			}
			setContentView(_view);
		}

		@Override
		public void onConnectionCompleted(ConnectTaskResult result) {
			_canStart = result.success;
			_view.setServerConnection(_connection);
			if(result.success)
			{
				_statusTextView.setText("Connection successful!");
				Log.v(TAG, "Handshake successful!");
			} else
			{
				_statusTextView.setText("Connection failed :(");
				Log.v(TAG, "Failed to connect to server");
			}
			
		}
		
		@Override
		public boolean dispatchKeyEvent(KeyEvent event) {
			int action = event.getAction();
			int keyCode = event.getKeyCode();
			switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (action == KeyEvent.ACTION_DOWN) {
					_view.volumePressed();
				}
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (action == KeyEvent.ACTION_DOWN) {
					_view.volumePressed();
				}
				return true;
			}
			return super.dispatchKeyEvent(event);
		}

}
