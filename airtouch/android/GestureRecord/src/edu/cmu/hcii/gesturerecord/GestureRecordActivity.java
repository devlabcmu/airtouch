package edu.cmu.hcii.gesturerecord;

import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;
import edu.cmu.hcii.airtouchlib.AirTouchDollarRecognizer;
import edu.cmu.hcii.airtouchlib.AirTouchMainActivityBase;

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
	
	public void checkClicked(View v)
	{
		boolean vertical = ((CheckBox)findViewById(R.id.checkBoxVertical)).isChecked() ;
		if(vertical)
		{	
			_airTouchView.getAirTouchRecognizer().getDollarRecognizer().clearTemplates();
			_airTouchView.getAirTouchRecognizer().loadGestureSet("updown");
			_airTouchView.getAirTouchRecognizer().setBufferDurationMs(700);
		}else
		{
			_airTouchView.getAirTouchRecognizer().getDollarRecognizer().clearTemplates();
			_airTouchView.getAirTouchRecognizer().loadGestureSet("default");
			_airTouchView.getAirTouchRecognizer().setBufferDurationMs(700);
		}
		((AirTouchDollarRecognizer)_airTouchView.getAirTouchRecognizer()).setDepthGesture( vertical );
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
