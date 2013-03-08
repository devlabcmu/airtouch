package edu.cmu.hcii.airtouchview;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.AttributeSet;
import edu.cmu.hcii.airtouchlib.AirTouchRecognizer.AirTouchType;
import edu.cmu.hcii.airtouchlib.AirTouchViewBase;
import edu.cmu.hcii.airtouchlib.PMDConstants;
import edu.cmu.hcii.airtouchlib.PMDDataHandler;
import edu.cmu.hcii.airtouchlib.SendReceiveTask;


@SuppressLint("UseSparseArrays")
public class AirTouchView extends AirTouchViewBase{
	static String TAG = "AirTouch.AirTouchView";
	Bitmap _pmdDepth = Bitmap.createBitmap(PMDConstants.PMD_NUM_COLS, PMDConstants.PMD_NUM_ROWS, Bitmap.Config.ARGB_8888);
	
	Matrix _depthMatrix = new Matrix();
	boolean _getOnlyFingerData = false;
	
	public AirTouchView(Context context) {
		super(context);
	}

	public AirTouchView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirTouchView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	
	public void setGetOnlyFingerData(boolean shouldIGetFingerData)
	{
		_getOnlyFingerData = shouldIGetFingerData;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		
		_errorText = null;

		_depthMatrix.setScale(2.0f, -2.0f);
		_depthMatrix.postTranslate(0, 240);
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
