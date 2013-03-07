package edu.cmu.hcii.dollarrecognizertest;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class DollarRecognizerActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(new DollarRecognizerView(this));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_dollar_recognizer, menu);
		return true;
	}

}
