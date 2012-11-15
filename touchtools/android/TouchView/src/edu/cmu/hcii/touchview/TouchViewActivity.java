package edu.cmu.hcii.touchview;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class TouchViewActivity extends Activity {
	public static final String TAG = "TouchView";
	TouchViewView _touchView;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Create");
        _touchView = new TouchViewView(this);
        setContentView(_touchView);
    }
    
	
    @Override
    protected void onPause() {
    	super.onPause();
    	Log.v(TAG, "Pause");
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.v(TAG, "Resume");
    }

}
