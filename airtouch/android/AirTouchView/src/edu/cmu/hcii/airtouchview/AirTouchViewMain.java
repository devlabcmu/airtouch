package edu.cmu.hcii.airtouchview;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import edu.cmu.hcii.airtouchlib.AirTouchMainActivityBase;
import edu.cmu.hcii.airtouchlib.AirTouchViewBase;
import edu.cmu.hcii.airtouchlib.PMDServerConnection;


public class AirTouchViewMain extends AirTouchMainActivityBase {
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

	boolean _canStart = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		_airTouchView = new AirTouchView(this);
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (action == KeyEvent.ACTION_DOWN) {
				((AirTouchView)_airTouchView).volumeUpPressed();
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (action == KeyEvent.ACTION_DOWN) {
				((AirTouchView)_airTouchView).volumeDownPressed();
			}
			return true;
		}
		return super.dispatchKeyEvent(event);
	}
	

}
