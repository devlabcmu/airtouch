package edu.cmu.hcii.airtouchpaint;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MainActivity extends Activity {
	private AirTouchPaintView m_view;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		m_view = new AirTouchPaintView(this);
		setContentView(new AirTouchPaintView(this));
	}


}
