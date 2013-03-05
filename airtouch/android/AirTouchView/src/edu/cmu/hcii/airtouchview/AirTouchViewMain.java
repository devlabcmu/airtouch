package edu.cmu.hcii.airtouchview;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import edu.cmu.hcii.airtouchlib.ConnectTask;
import edu.cmu.hcii.airtouchlib.ConnectTaskCompletedHandler;
import edu.cmu.hcii.airtouchlib.ConnectTaskResult;
import edu.cmu.hcii.airtouchlib.PMDServerConnection;


public class AirTouchViewMain extends Activity implements ConnectTaskCompletedHandler {
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

	AirTouchView _airTouchView;
	boolean _canStart = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_airtouchview_main);

		_ipEditText = (EditText)findViewById(R.id.editTextIP);
		_portEditText = (EditText)findViewById(R.id.editTextPort);
		_statusTextView = (TextView)findViewById(R.id.textViewStatus);
		_portEditText.setText(Integer.toString(PMDServerConnection.DEFAULT_SERVER_PORT));
		_ipEditText.setText(PMDServerConnection.DEFAULT_IP_STRING);

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
		_statusTextView.setText("Disconnected");
		// TODO actally disconnect here
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
		_statusTextView.setText("Connecting...");
		_connection = new PMDServerConnection(_ipEditText.getText().toString(), _portEditText.getText().toString() );
		new ConnectTask(this, _connection).execute();
	}

	public void viewFingerDataClicked(View v)
	{
		if(!_canStart){

			_statusTextView.setText("you must first connect!");
			return;
		}
		_airTouchView.shouldIGetOnlyFingerData(true);
		setContentView(_airTouchView);
	}

	public void viewRawClicked(View v)
	{
		if(!_canStart){

			_statusTextView.setText("you must first connect!");
			return;
		}
		_airTouchView.shouldIGetOnlyFingerData(false);
		setContentView(_airTouchView);
	}

	@Override
	public void onConnectionCompleted(ConnectTaskResult result) {
		_canStart = result.success;
		_airTouchView.setServerConnection(_connection);
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

}
