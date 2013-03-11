package edu.cmu.hcii.airtouchview;

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
import edu.cmu.hcii.airtouchlib.AirTouchMainActivityBase;
import edu.cmu.hcii.airtouchlib.BindTask;
import edu.cmu.hcii.airtouchlib.ConnectTaskCompletedHandler;
import edu.cmu.hcii.airtouchlib.ConnectTaskResult;
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

	AirTouchView _airTouchView;
	boolean _canStart = false;

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (action == KeyEvent.ACTION_DOWN) {
				_airTouchView.volumeUpPressed();
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (action == KeyEvent.ACTION_DOWN) {
				_airTouchView.volumeDownPressed();
			}
			return true;
		}
		return super.dispatchKeyEvent(event);
	}
	

}
