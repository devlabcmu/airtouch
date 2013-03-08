package edu.cmu.hcii.airtouchview;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import lx.interaction.dollar.Result;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import edu.cmu.hcii.airtouchlib.AirTouchDollarRecognizer;
import edu.cmu.hcii.airtouchlib.AirTouchPoint;
import edu.cmu.hcii.airtouchlib.AirTouchPoint.TouchType;
import edu.cmu.hcii.airtouchlib.AirTouchRecognizer;
import edu.cmu.hcii.airtouchlib.AirTouchRecognizer.AirTouchType;
import edu.cmu.hcii.airtouchlib.DisconnectTask;
import edu.cmu.hcii.airtouchlib.PMDConstants;
import edu.cmu.hcii.airtouchlib.PMDDataHandler;
import edu.cmu.hcii.airtouchlib.PMDFinger;
import edu.cmu.hcii.airtouchlib.PMDServerConnection;
import edu.cmu.hcii.airtouchlib.SendReceiveTask;
import edu.cmu.hcii.airtouchlib.SendReceiveTask.PMDSendData;


@SuppressLint("UseSparseArrays")
public class AirTouchView extends View implements PMDDataHandler {

	// Constants
	static final int TOUCH_SCALE = 50;
	

	static String TAG = "AirTouch.AirTouchView";

	// static variables
	private static Map<AirTouchPoint.TouchType, Paint> paintBrushes = new HashMap<AirTouchPoint.TouchType, Paint>();
	static Paint defaultPaintBrush;
	static Paint textPaintBrush;

	// Networking
	PMDServerConnection _connection;
	PMDSendData _dataFromServer;
	boolean _stopNetworkConnection = false;
	boolean _getOnlyFingerData = false;
	Timer _timer;

	// UI
	private Map<Integer, AirTouchPoint> _touchMap = new HashMap<Integer, AirTouchPoint>();
	Object _touchMapLock = new Object();
	String _errorText;
	RectF _touchDrawRect = new RectF();
	Bitmap _pmdDepth = Bitmap.createBitmap(PMDConstants.PMD_NUM_COLS, PMDConstants.PMD_NUM_ROWS, Bitmap.Config.ARGB_8888);
	Matrix _depthMatrix = new Matrix();
	Path gesturePath = new Path();
	
	// Gestures
	AirTouchDollarRecognizer _airTouchRecognizer = new AirTouchDollarRecognizer(1000, AirTouchType.BEFORE_TOUCH);
	
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
	
	protected void onDraw(Canvas canvas) {
		canvas.drawRGB(255, 255, 255);
		Paint paint;

		Map<Integer, LinkedList<PMDFinger>> gestureBuffers = _airTouchRecognizer.getAllGestureBuffers();
		// Draw touch points
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


		// draw a bitmap with the bytes if we are not just showing finger data.
		if(!_getOnlyFingerData) canvas.drawBitmap(_pmdDepth, _depthMatrix, null);
		

		if(_touchMap.values().size() == 0 && _dataFromServer !=null)
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
		} else
		{
			
			for (Entry<Integer, LinkedList<PMDFinger>> paths : gestureBuffers.entrySet()) {
				gesturePath.reset();
				TouchType type = paths.getKey() % 2 == 0 ? TouchType.AIR_MOVE1 : TouchType.AIR_MOVE2;
				
				boolean first = true;
				for (PMDFinger p : paths.getValue()) {
					PMDFinger screenSpace = phoneToScreen(p);
					if(first){
						first = false;
						gesturePath.moveTo(screenSpace.x, screenSpace.y);
					} else
					{
						gesturePath.lineTo(screenSpace.x, screenSpace.y);
					}
				}
				paintBrushes.get(type).setAlpha(255);
				canvas.drawPath(gesturePath, paintBrushes.get(type));
			}	
		}

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

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		_airTouchRecognizer.setScreenHeight(h);
		_airTouchRecognizer.setScreenWidth(w);
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

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		AirTouchPoint.TouchType type;
		int action = event.getActionMasked();

		switch(action)
		{
		case MotionEvent.ACTION_DOWN:
			type = AirTouchPoint.TouchType.TOUCH_DOWN;
			_airTouchRecognizer.onTouchDown(event);
			break;
		case MotionEvent.ACTION_UP:
			performClick();
			type = AirTouchPoint.TouchType.TOUCH_UP;
			_airTouchRecognizer.onTouchUp(event);
			postInvalidateDelayed(AirTouchRecognizer.AFTER_TOUCH_TIMEOUT_MS * 2);
			break;
		case MotionEvent.ACTION_MOVE:
			type = AirTouchPoint.TouchType.TOUCH_MOVE;
			
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

		_depthMatrix.setScale(2.0f, -2.0f);
		_depthMatrix.postTranslate(0, 240);
		final PMDDataHandler me = this;
		//_depthMatrix.setScale(2.0f, 2.0f);
		// handshake has already happened
		// begin sending and receiving data
		TimerTask task = new TimerTask() {
            public void run() {
                new SendReceiveTask(_connection, _getOnlyFingerData, me, _airTouchRecognizer).execute();
                
            }
        };
        _timer = new Timer();
        _timer.scheduleAtFixedRate(task, 1, 20);
	}
	


	public void stop()
	{
		_stopNetworkConnection = true;
		_timer.cancel();
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
	


}
