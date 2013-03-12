package edu.cmu.hcii.gesturerecord;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import edu.cmu.hcii.airtouchlib.AirTouchMainActivityBase;
import edu.cmu.hcii.airtouchlib.AirTouchViewBase;
import edu.cmu.hcii.gesturerecord.R;

public class GestureRecordActivity  extends AirTouchMainActivityBase {
	static final String LOG_TAG="AirTouch.GestureRecordActivity";
	public void viewFingerDataClicked(View v)
	{
		if(!_canStart){

			_statusTextView.setText("you must first connect!");
			return;
		}
		
		setContentView(R.layout.view_fingers);
		
		FrameLayout frame = (FrameLayout)findViewById(R.id.airTouchViewFrame);
		if(frame == null)
			Log.e(LOG_TAG, "airtouchframe is null!");
		
		frame.addView(_airTouchView);
	}
	
	public void saveClicked(View v)
	{
		Log.i(LOG_TAG, "saving last gesture...");
		// filename will be gesture + timestamp + xml
		String gestureName = ((TextView)findViewById(R.id.gestureNameText1)).getText().toString();
		_airTouchView.getAirTouchRecognizer().saveBufferToGesture(gestureName);
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
