package edu.cmu.hcii.airtouchview;

import android.os.Bundle;
import android.view.KeyEvent;
import edu.cmu.hcii.airtouchlib.AirTouchMainActivityBase;


public class AirTouchViewMain extends AirTouchMainActivityBase {
	// Constants
	static final String TAG = "AirTouchView"; 

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
				_airTouchView.getAirTouchRecognizer().changeAirTouchType(false);
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (action == KeyEvent.ACTION_DOWN) {
				_airTouchView.getAirTouchRecognizer().changeAirTouchType(true);
			}
			return true;
		}
		return super.dispatchKeyEvent(event);
	}
	

}
