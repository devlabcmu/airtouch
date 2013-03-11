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
import edu.cmu.hcii.airtouchlib.AirTouchMainActivityBase;
import edu.cmu.hcii.airtouchlib.AirTouchViewBase;
import edu.cmu.hcii.airtouchlib.BindTask;
import edu.cmu.hcii.airtouchlib.ConnectTaskCompletedHandler;
import edu.cmu.hcii.airtouchlib.ConnectTaskResult;
import edu.cmu.hcii.airtouchlib.PMDServerConnection;

public class MainActivity extends AirTouchMainActivityBase {
	
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			_airTouchView = new AirTouchPaintView(this);
		}

		@Override
		public boolean dispatchKeyEvent(KeyEvent event) {
			AirTouchPaintView v = (AirTouchPaintView) _airTouchView;
			int action = event.getAction();
			int keyCode = event.getKeyCode();
			switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (action == KeyEvent.ACTION_DOWN) {
					v.volumePressed();
				}
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (action == KeyEvent.ACTION_DOWN) {
					v.volumePressed();
				}
				return true;
			}
			return super.dispatchKeyEvent(event);
		}

}
