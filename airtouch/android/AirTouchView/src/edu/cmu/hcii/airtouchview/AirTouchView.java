package edu.cmu.hcii.airtouchview;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import lx.interaction.dollar.Point;
import lx.interaction.dollar.Utils;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import edu.cmu.hcii.airtouchlib.AirTouchDollarRecognizer;
import edu.cmu.hcii.airtouchlib.AirTouchRecognizer.AirTouchType;
import edu.cmu.hcii.airtouchlib.AirTouchViewBase;


@SuppressLint("UseSparseArrays")
public class AirTouchView extends AirTouchViewBase{
	static String TAG = "AirTouch.AirTouchView";
	
	public AirTouchView(Context context) {
		super(context);
	}

	public AirTouchView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirTouchView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		
		_errorText = null;
	}
	
	public void volumeUpPressed()
	{
		int ti = _airTouchRecognizer.getAirTouchType().ordinal();
		int ni;
		ni = ti + 1;
		if(ni >= AirTouchType.values().length) ni = 0;
		_airTouchRecognizer.setAirTouchType(AirTouchType.values()[ni]);
	}

	public void volumeDownPressed()
	{
		int ti = _airTouchRecognizer.getAirTouchType().ordinal();
		int ni;
		ni = ti - 1;
		if(ni < 0 ) ni = AirTouchType.values().length - 1;
		_airTouchRecognizer.setAirTouchType(AirTouchType.values()[ni]);
	}
}
