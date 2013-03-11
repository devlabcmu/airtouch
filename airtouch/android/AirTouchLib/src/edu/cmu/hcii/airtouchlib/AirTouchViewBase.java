package edu.cmu.hcii.airtouchlib;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import lx.interaction.dollar.DollarRecognizer;
import lx.interaction.dollar.Result;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import edu.cmu.hcii.airtouchlib.AirTouchPoint.TouchType;
import edu.cmu.hcii.airtouchlib.AirTouchRecognizer.AirTouchType;
import edu.cmu.hcii.airtouchlib.SendReceiveTask.PMDSendData;

public class AirTouchViewBase extends View implements PMDDataHandler {

	// Constants
	static final int TOUCH_SCALE = 50;


	static String TAG = "AirTouch.AirTouchViewBase";

	// static variables
	private static Map<AirTouchPoint.TouchType, Paint> paintBrushes = new HashMap<AirTouchPoint.TouchType, Paint>();
	static Paint defaultPaintBrush;
	static Paint textPaintBrush;

	// Networking
	protected PMDServerConnection _connection;
	protected PMDSendData _dataFromServer;
	protected boolean _stopNetworkConnection = false;
	
	protected Timer _timer;

	// UI
	private Map<Integer, AirTouchPoint> _touchMap = new HashMap<Integer, AirTouchPoint>();
	protected Object _touchMapLock = new Object();
	protected String _errorText;
	protected RectF _touchDrawRect = new RectF();
	
	
	// Show debug info
	public boolean _showTouches = false;
	public boolean _showFingersInAir = true;
	public boolean _showAirGestures = true;
	public boolean _showDebugText = true;
	

	// Gestures
	protected  AirTouchDollarRecognizer _airTouchRecognizer;

	static
	{
		defaultPaintBrush = new Paint();
		defaultPaintBrush.setColor(Color.BLACK);
		defaultPaintBrush.setTextSize(40);

		textPaintBrush = new Paint();
		textPaintBrush.setColor(Color.RED);
		textPaintBrush.setTextSize(40);

		Paint paint;

		paint = new Paint();
		paint.setColor(Color.MAGENTA);
		paint.setStyle(Style.STROKE);
		paintBrushes.put(AirTouchPoint.TouchType.TOUCH_DOWN, paint);

		paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setStyle(Style.STROKE);
		paintBrushes.put(AirTouchPoint.TouchType.TOUCH_MOVE, paint);

		paint = new Paint();
		paint.setColor(Color.BLUE);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(5);
		paintBrushes.put(AirTouchPoint.TouchType.AIR_MOVE1, paint);

		paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(5);
		paintBrushes.put(AirTouchPoint.TouchType.AIR_MOVE2, paint);

	}

	public AirTouchViewBase(Context context) {
		super(context);
	}

	public AirTouchViewBase(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirTouchViewBase(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setServerConnection(PMDServerConnection c)
	{
		_connection = c;
	}

	public PMDFinger phoneToScreen(PMDFinger f)
	{
		PMDFinger result = new PMDFinger(f);
		float realDepth = 0.1f;
		WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();

		int width = display.getWidth();
		int height = display.getHeight();

		result.x = f.x  * width;
		result.y = f.z  * height;
		result.z = f.y / realDepth;
		return result;
	}

	protected void drawTouchPoints(Canvas canvas)
	{
		Paint paint;

		synchronized(_touchMapLock) {

			for(AirTouchPoint tp : _touchMap.values()) {
				canvas.save();

				canvas.translate(tp.x, tp.y);
				paint = paintBrushes.get(tp.type);
				if(paint == null) paint = defaultPaintBrush;
				_touchDrawRect.set(
						-TOUCH_SCALE, -TOUCH_SCALE,
						TOUCH_SCALE, TOUCH_SCALE);
				canvas.drawOval(_touchDrawRect, paint);

				canvas.restore();
			}
		}
	}
	
	protected void drawFingersInAir(Canvas canvas)
	{
		for (int i = 0; i < _dataFromServer.fingers.length; i++) {
			if(_dataFromServer.fingers[i] == null) continue;

			if(_dataFromServer.fingers[i].id >= 0)
			{

				PMDFinger p = phoneToScreen(_dataFromServer.fingers[i]);

				TouchType type = _dataFromServer.fingers[i].id % 2 == 0 ? TouchType.AIR_MOVE1 : TouchType.AIR_MOVE2;
				paintBrushes.get(type).setAlpha((int)(1 - 255 * Math.max(0, Math.min(1, p.z) ) ));

				canvas.drawCircle(p.x, p.y, p.z * 500, paintBrushes.get(type));	
				paintBrushes.get(type).setAlpha(1);
			}
		} 
	}
	
	protected void drawDebugText(Canvas canvas)
	{
		canvas.save();

		canvas.translate(20, 50);
		// Draw any errors
		if(_errorText != null) 
		{
			canvas.drawText(_errorText, 0, 0, textPaintBrush);
		}
		canvas.translate(0,50);
		canvas.drawText(String.format("pmd data fps: %.2f", _pmdFPS), 0, 0, defaultPaintBrush);
		canvas.translate(0,50);
		canvas.drawText(_airTouchRecognizer.getAirTouchType().toString(), 0, 0, defaultPaintBrush);
		for (Entry<Integer, Result> result : _airTouchRecognizer.getGestureResults().entrySet())
		{
			canvas.translate(0, 50);
			canvas.drawText(String.format("%s score: %.2f", result.getValue().Name, result.getValue().Score),0,0,defaultPaintBrush);

		}
		canvas.restore();
	}
	
	protected void onDraw(Canvas canvas) {
		canvas.drawRGB(255, 255, 255);

		
		// Draw touch points
		if(_showTouches) drawTouchPoints(canvas);

		if(_showFingersInAir &&  _dataFromServer !=null) drawFingersInAir(canvas);
			
		if(_showAirGestures) _airTouchRecognizer.drawGesture(canvas, paintBrushes);
			
		if(_showDebugText) drawDebugText(canvas);
		
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		_airTouchRecognizer.setScreenHeight(h);
		_airTouchRecognizer.setScreenWidth(w);
	}

	
	protected void onTouchDown(MotionEvent event){}
	protected void onTouchMove(MotionEvent event){}
	protected void onTouchUp(MotionEvent event){}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		AirTouchPoint.TouchType type;
		int action = event.getActionMasked();

		switch(action)
		{
		case MotionEvent.ACTION_DOWN:
			type = AirTouchPoint.TouchType.TOUCH_DOWN;
			_airTouchRecognizer.onTouchDown(event);
			onTouchDown(event);
			break;
		case MotionEvent.ACTION_UP:
			performClick();
			type = AirTouchPoint.TouchType.TOUCH_UP;
			_airTouchRecognizer.onTouchUp(event);
			onTouchUp(event);
			postInvalidateDelayed(AirTouchRecognizer.AFTER_TOUCH_TIMEOUT_MS * 2);
			
			break;
		case MotionEvent.ACTION_MOVE:
			type = AirTouchPoint.TouchType.TOUCH_MOVE;
			onTouchMove(event);
			break;
		default:
			return false;
		}

		for (int i = 0; i < event.getPointerCount(); i++) 
		{
			int id = event.getPointerId(i);

			float x = event.getX(i);
			float y = event.getY(i);
			synchronized(_touchMapLock)
			{
				if(action != MotionEvent.ACTION_UP)
				{
					_touchMap.put(id, new AirTouchPoint(x, y, type));	
				} 
				else 
				{
					if(_touchMap.containsKey(id)){
						_touchMap.remove(id);
					}
				}				
			}
		}
		postInvalidate();
		return true;

	}
	
	

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		_errorText = null;
		 
		// TODO: load a gesture set from file
		_airTouchRecognizer = new AirTouchDollarRecognizer(1000, AirTouchType.BEFORE_TOUCH, DollarRecognizer.GESTURES_SIMPLE);
		// try loading default gesture set
		_airTouchRecognizer.loadGestureSet("default");
		
//		am.close();
		beginReceivingData();
	}

	
	protected void beginReceivingData()
	{
		final PMDDataHandler me = this;
		// handshake has already happened
		// begin sending and receiving data
		TimerTask task = new TimerTask() {
			public void run() {
				new SendReceiveTask(_connection, true, me, _airTouchRecognizer).execute();

			}
		};
		_timer = new Timer();
		_timer.scheduleAtFixedRate(task, 1, 20);	
	}


	public void stop()
	{
		_stopNetworkConnection = true;
		if(_timer != null)
			_timer.cancel();
		if(_connection != null)
			new DisconnectTask(_connection).execute();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		// garbage collect the pmd data
		_dataFromServer = null;
		_stopNetworkConnection = true;
		// stop sending and receiving data
		// send disconnect message?
	}



	long _lastPmdDataTime = 0;
	float _pmdFPS = 0;
	int _fpsCounter = 0;
	long _pmdInterval = 0;

	public void updateFPS()
	{
		long now = System.currentTimeMillis();
		if(_lastPmdDataTime != 0){
			_pmdInterval = (now - _lastPmdDataTime);
			if(_pmdInterval > 1000)
			{
				_pmdFPS = _fpsCounter;
				_fpsCounter = 0;
				_lastPmdDataTime = now;
			}
		} else
		{
			_lastPmdDataTime = now;	
		}
		_fpsCounter++;
	}

	@Override
	public void newPMDData(PMDSendData data) {
		_dataFromServer = data;
		updateFPS();
		postInvalidate();
	}

	@Override
	public void onSendReceiveTaskFailed(String message) {
		// TODO Auto-generated method stub

	}

	
	public AirTouchDollarRecognizer getAirTouchRecognizer()
	{
		return _airTouchRecognizer;
	}


}
