package edu.cmu.hcii.airtouchlib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import lx.interaction.dollar.Result;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
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
	private static Map<AirTouchPoint.TouchType, Paint> g_paintBrushes = new HashMap<AirTouchPoint.TouchType, Paint>();
	protected static Paint g_defaultPaintBrush;
	protected static Paint g_textPaintBrush;

	// Networking
	protected PMDServerConnection m_connection;
	protected PMDSendData m_dataFromServer;
	protected boolean m_stopNetworkConnection = false;

	protected Timer _timer;

	// UI
	private Map<Integer, AirTouchPoint> m_touchMap = new HashMap<Integer, AirTouchPoint>();
	protected Object m_touchMapLock = new Object();
	protected String m_errorText;
	protected RectF m_touchDrawRect = new RectF();


	// Show debug info
	public boolean m_showTouches = false;
	public boolean m_showFingersInAir = true;
	public boolean m_showAirGestures = true;
	public boolean m_showDebugText = true;


	// Gestures
	protected List<AirTouchRecognizer> m_airTouchRecognizers = new ArrayList<AirTouchRecognizer>();
	protected  AirTouchDollarRecognizer m_airTouchRecognizer;

	static
	{
		g_defaultPaintBrush = new Paint();
		g_defaultPaintBrush.setColor(Color.BLACK);
		g_defaultPaintBrush.setTextSize(40);

		g_textPaintBrush = new Paint();
		g_textPaintBrush.setColor(Color.RED);
		g_textPaintBrush.setTextSize(40);

		Paint paint;

		paint = new Paint();
		paint.setColor(Color.MAGENTA);
		paint.setStyle(Style.STROKE);
		g_paintBrushes.put(AirTouchPoint.TouchType.TOUCH_DOWN, paint);

		paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setStyle(Style.STROKE);
		g_paintBrushes.put(AirTouchPoint.TouchType.TOUCH_MOVE, paint);

		paint = new Paint();
		paint.setColor(Color.BLUE);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(5);
		g_paintBrushes.put(AirTouchPoint.TouchType.AIR_MOVE1, paint);

		paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(5);
		g_paintBrushes.put(AirTouchPoint.TouchType.AIR_MOVE2, paint);

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
		m_connection = c;
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

		synchronized(m_touchMapLock) {

			for(AirTouchPoint tp : m_touchMap.values()) {
				canvas.save();

				canvas.translate(tp.x, tp.y);
				paint = g_paintBrushes.get(tp.type);
				if(paint == null) paint = g_defaultPaintBrush;
				m_touchDrawRect.set(
						-TOUCH_SCALE, -TOUCH_SCALE,
						TOUCH_SCALE, TOUCH_SCALE);
				canvas.drawOval(m_touchDrawRect, paint);

				canvas.restore();
			}
		}
	}

	protected void drawFingersInAir(Canvas canvas)
	{
		for (int i = 0; i < m_dataFromServer.fingers.length; i++) {
			if(m_dataFromServer.fingers[i] == null) continue;

			if(m_dataFromServer.fingers[i].id >= 0)
			{

				PMDFinger p = phoneToScreen(m_dataFromServer.fingers[i]);

				TouchType type = m_dataFromServer.fingers[i].id % 2 == 0 ? TouchType.AIR_MOVE1 : TouchType.AIR_MOVE2;
				g_paintBrushes.get(type).setAlpha((int)(1 - 255 * Math.max(0, Math.min(1, p.z) ) ));

				canvas.drawCircle(p.x, p.y, p.z * 500, g_paintBrushes.get(type));	
				g_paintBrushes.get(type).setAlpha(1);
			}
		} 
	}

	protected void drawDebugText(Canvas canvas)
	{
		canvas.save();

		canvas.translate(20, 50);
		// Draw any errors
		if(m_errorText != null) 
		{
			canvas.drawText(m_errorText, 0, 0, g_textPaintBrush);
		}
		canvas.translate(0,50);
		canvas.drawText(String.format("pmd data fps: %.2f", _pmdFPS), 0, 0, g_defaultPaintBrush);
		canvas.translate(0,50);
		canvas.drawText(m_airTouchRecognizer.getAirTouchType().toString(), 0, 0, g_defaultPaintBrush);
		for (Entry<Integer, Result> result : m_airTouchRecognizer.getGestureResults().entrySet())
		{
			canvas.translate(0, 50);
			canvas.drawText(String.format("%s score: %.2f", result.getValue().Name, result.getValue().Score),0,0,g_defaultPaintBrush);

		}
		canvas.restore();
	}

	protected void onDraw(Canvas canvas) {
		canvas.drawRGB(255, 255, 255);


		// Draw touch points
		if(m_showTouches) drawTouchPoints(canvas);

		if(m_showFingersInAir &&  m_dataFromServer !=null) drawFingersInAir(canvas);

		if(m_showAirGestures) m_airTouchRecognizer.drawGesture(canvas, g_paintBrushes);

		if(m_showDebugText) drawDebugText(canvas);

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		for (AirTouchRecognizer r : m_airTouchRecognizers) {
			r.setScreenHeight(h);
			r.setScreenWidth(w);
		}
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
			for (AirTouchRecognizer r : m_airTouchRecognizers) {
				r.onTouchDown(event);
			}

			onTouchDown(event);
			break;
		case MotionEvent.ACTION_UP:
			performClick();
			type = AirTouchPoint.TouchType.TOUCH_UP;
			for (AirTouchRecognizer r : m_airTouchRecognizers) {
				r.onTouchUp(event);
			}
			_lastTouchUp = new PointF(event.getX(), event.getY());
			onTouchUp(event);
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
			synchronized(m_touchMapLock)
			{
				if(action != MotionEvent.ACTION_UP)
				{
					m_touchMap.put(id, new AirTouchPoint(x, y, type));	
				} 
				else 
				{
					if(m_touchMap.containsKey(id)){
						m_touchMap.remove(id);
					}
				}				
			}
		}
		if(action == MotionEvent.ACTION_UP && m_airTouchRecognizer.getAirTouchType() == AirTouchType.AFTER_TOUCH)
			invalidate();
		else
			postInvalidateDelayed(AirTouchRecognizer.AFTER_TOUCH_TIMEOUT_MS * 2);

		return true;

	}



	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		m_errorText = null;

		// TODO: load a gesture set from file
		m_airTouchRecognizer = new AirTouchDollarRecognizer(700, AirTouchType.BEFORE_TOUCH);
		// try loading default gesture set
		m_airTouchRecognizer.loadGestureSet("default");
		m_airTouchRecognizers.add(m_airTouchRecognizer);

		//		am.close();
		beginReceivingData();
	}

	PointF _lastTouchUp;
	protected void beginReceivingData()
	{
		final PMDDataHandler me = this;

		// handshake has already happened
		// begin sending and receiving data
		TimerTask task = new TimerTask() {
			public void run() {
				String message = "f";
				if(_lastTouchUp != null){
					float lastX = _lastTouchUp.x / (float)getWidth();
					float lastY = _lastTouchUp.y / (float)getHeight();
					message = "fp " + lastX + " " + lastY;
							_lastTouchUp = null;
				}
				SendReceiveTask toExecute =new SendReceiveTask(m_connection, message ,  me);
				for (AirTouchRecognizer r : m_airTouchRecognizers) {
					toExecute.addHandler(r);
				}
				toExecute.execute();
			}
		};
		_timer = new Timer();
		_timer.scheduleAtFixedRate(task, 1, 20);	
	}


	public void stop()
	{
		m_stopNetworkConnection = true;
		if(_timer != null)
			_timer.cancel();
		if(m_connection != null)
			new DisconnectTask(m_connection).execute();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		// garbage collect the pmd data
		m_dataFromServer = null;
		m_stopNetworkConnection = true;
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
		m_dataFromServer = data;
		updateFPS();
		postInvalidate();
	}

	@Override
	public void onSendReceiveTaskFailed(String message) {
		// TODO Auto-generated method stub

	}



	public AirTouchDollarRecognizer getAirTouchRecognizer()
	{
		return m_airTouchRecognizer;
	}


}
