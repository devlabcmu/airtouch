package edu.cmu.hcii.airtouchpaint;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import lx.interaction.dollar.Point;
import lx.interaction.dollar.Result;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import edu.cmu.hcii.airtouchlib.AirTouchDollarRecognizer;
import edu.cmu.hcii.airtouchlib.AirTouchPoint;
import edu.cmu.hcii.airtouchlib.AirTouchPoint.TouchType;
import edu.cmu.hcii.airtouchlib.AirTouchRecognizer;
import edu.cmu.hcii.airtouchlib.AirTouchRecognizer.AirTouchType;
import edu.cmu.hcii.airtouchlib.AirTouchViewBase;
import edu.cmu.hcii.airtouchlib.FingerLiftedRecognizer;
import edu.cmu.hcii.airtouchlib.PMDDataHandler;
import edu.cmu.hcii.airtouchlib.PMDFinger;
import edu.cmu.hcii.airtouchlib.SendReceiveTask;

public class AirTouchPaintView extends AirTouchViewBase {
	enum Command {STROKE, RECT, CLEAR, CLIP};

	static String LOG_TAG = "AirTouchPaintView";
	protected static Map<AirTouchPoint.TouchType, Paint> g_gestureBrushes = new HashMap<AirTouchPoint.TouchType, Paint>();
	static Paint g_shadowPaint;
	static final int UNDO_HISTORY_SIZE = 5;

	static
	{
		Paint paint;

		paint = new Paint();
		paint.setColor(Color.LTGRAY);
		g_shadowPaint = paint;
		
		paint = new Paint();
		paint.setColor(Color.MAGENTA);
		paint.setStyle(Style.STROKE);
		g_gestureBrushes.put(AirTouchPoint.TouchType.TOUCH_DOWN, paint);

		paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setStyle(Style.STROKE);
		g_gestureBrushes.put(AirTouchPoint.TouchType.TOUCH_MOVE, paint);

		paint = new Paint();
		paint.setColor(Color.BLUE);
		paint.setStyle(Style.STROKE);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeWidth(5);
		g_gestureBrushes.put(AirTouchPoint.TouchType.AIR_MOVE1, paint);

		paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStyle(Style.STROKE);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeWidth(5);
		g_gestureBrushes.put(AirTouchPoint.TouchType.AIR_MOVE2, paint);

	}
	
	LinkedList<Bitmap> m_bitmapHistory = new LinkedList<Bitmap>();
	Bitmap m_bitmap;
	Canvas m_canvas;
	
	long m_lastTouchDownMs;
	Point m_lastTouchPoint = new Point(0,0);
	
	Bitmap m_background;
	
	// very simple one-level view
	
	GraphicalObject m_currentObject;
	GraphicalObject m_lastObject;
	
	
	// after touch
	Timer m_afterTouchTimer = new Timer();
	
	public AirTouchPaintView(Context context) {
		super(context);
	}

	public AirTouchPaintView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirTouchPaintView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	protected  AirTouchDollarRecognizer m_beforeTouchRecognizer = new AirTouchDollarRecognizer(700, AirTouchType.BEFORE_TOUCH);
	protected  AirTouchDollarRecognizer m_betweenTouchRecognizer = new AirTouchDollarRecognizer(700, AirTouchType.BETWEEN_TOUCHES);
	protected FingerLiftedRecognizer m_afterTouchRecognizer = new FingerLiftedRecognizer(700, AirTouchType.AFTER_TOUCH);
	
	@Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		m_beforeTouchRecognizer.loadGestureSet("circle");
		m_betweenTouchRecognizer.loadGestureSet("caret");
		
		defaultPaintBrush.setColor(Color.WHITE);
		_airTouchRecognizer.setAirTouchType(AirTouchType.BETWEEN_TOUCHES);
	}
	
	@Override
	protected void beginReceivingData() {
		final PMDDataHandler me = this;

		// handshake has already happened
		// begin sending and receiving data
		TimerTask task = new TimerTask() {
			public void run() {
				new SendReceiveTask(_connection, true, me, _airTouchRecognizer, m_beforeTouchRecognizer,m_betweenTouchRecognizer, m_afterTouchRecognizer).execute();

			}
		};
		_timer = new Timer();
		_timer.scheduleAtFixedRate(task, 1, 20);
	}
	
	private Command getCommand()
	{
		Map<Integer, Result> gestureResults = m_betweenTouchRecognizer.getGestureResults();
		// if either finger has recognized caret
		for (Entry<Integer, Result> result : gestureResults.entrySet()) {
			if(result.getValue().Name.contains("caret") && result.getValue().Score > 0.8) return Command.RECT;
		}
		
		gestureResults = m_beforeTouchRecognizer.getGestureResults();
		// if either finger has recognized caret
		for (Entry<Integer, Result> result : gestureResults.entrySet()) {
			if(result.getValue().Name.contains("circle") && result.getValue().Score > 0.8) return Command.CLEAR;
		}
		return Command.STROKE;
	}
	// OnTouch* events occur after gesture recognition for touch events has been processed
	
	@Override
	protected void onTouchDown(MotionEvent event)
	{
		m_beforeTouchRecognizer.onTouchDown(event);
		m_betweenTouchRecognizer.onTouchDown(event);
		m_afterTouchRecognizer.onTouchDown(event);
		Command command = getCommand();
		if(command == Command.RECT)
		{
			undo();
			m_currentObject = new Rectangle((float)m_lastTouchPoint.X, (float)m_lastTouchPoint.Y, event.getX(), event.getY());
		} else if(command == Command.STROKE)
		{
			m_currentObject = new Stroke();	
			// check if we are supposed to be making a rectangle
			m_currentObject.onTouchDown(event);
		} else if(command == Command.CLEAR)
		{
			addBitmapToHistory();
			if(m_background != null)
				m_canvas.drawBitmap(m_background, 0,0, new Paint());
			m_currentObject = null;
		}
		updateLastPoint(event);
	}
	
	protected void onTouchMove(MotionEvent event)
	{
		if(m_currentObject != null){
			m_currentObject.onTouchMove(event);
		}
		updateLastPoint(event);
	}
	
	protected void onTouchUp(MotionEvent event)
	{
		m_beforeTouchRecognizer.onTouchUp(event);
		m_betweenTouchRecognizer.onTouchUp(event);
		m_afterTouchRecognizer.onTouchUp(event);
		if(m_currentObject != null)
		{
			addBitmapToHistory();
			// rasterize the current object
			m_currentObject.draw(m_canvas);
		}
		m_lastObject = m_currentObject;
		m_currentObject = null;
		m_afterTouchTimer.schedule(new TimerTask(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Map<Integer, Result> gestureResults = m_afterTouchRecognizer.getGestureResults();
				// if either finger has recognized caret
				for (Entry<Integer, Result> result : gestureResults.entrySet()) {
					if(result.getValue().Name.contains("finger") && result.getValue().Score > 0.8)
					{
						Log.i(LOG_TAG, "finger lifted detected!");
						if(m_lastObject != null){
							undo();
							if(m_lastObject.getColor() == Color.BLACK)
							{
								m_lastObject.setColor(Color.WHITE);
							}else
							{
								m_lastObject.setColor(Color.BLACK);
							}
							m_lastObject.draw(m_canvas);
						}
					}
					postInvalidate();
				}
			}
			
		}, AirTouchRecognizer.AFTER_TOUCH_TIMEOUT_MS );
	}
	
	private void addBitmapToHistory()
	{
		// save the bitmap
		while(m_bitmapHistory.size() > UNDO_HISTORY_SIZE)
		{
			m_bitmapHistory.removeFirst();
		}
		m_bitmapHistory.add(Bitmap.createBitmap(m_bitmap));

	}
	
	private void updateLastPoint(MotionEvent e)
	{
		m_lastTouchPoint.X = e.getX();
		m_lastTouchPoint.Y = e.getY();
	}
	
	protected void drawFingersInAir(Canvas canvas)
	{
		for (int i = 0; i < _dataFromServer.fingers.length; i++) {
			if(_dataFromServer.fingers[i] == null) continue;

			if(_dataFromServer.fingers[i].id >= 0)
			{

				PMDFinger p = phoneToScreen(_dataFromServer.fingers[i]);

				TouchType type = _dataFromServer.fingers[i].id % 2 == 0 ? TouchType.AIR_MOVE1 : TouchType.AIR_MOVE2;
				g_shadowPaint.setAlpha((int)(1 - 255 * Math.max(0, Math.min(1, p.z) ) ));

				canvas.drawCircle(p.x, p.y, p.z * 500,g_shadowPaint);	
				g_shadowPaint.setAlpha(1);
			}
		} 
	}
	
	public static Bitmap getBitmapFromAsset(Context context, String strName) {
	    AssetManager assetManager = context.getAssets();

	    InputStream istr;
	    Bitmap bitmap = null;
	    try {
	        istr = assetManager.open(strName);
	        bitmap = BitmapFactory.decodeStream(istr);
	    } catch (IOException e) {
	    	Log.e(LOG_TAG, e.getMessage());
	        return null;
	    }

	    return bitmap;
	}
	
	private void initBitmap(int width, int height)
	{
		Log.i(LOG_TAG, "initializing bitmap...");
		m_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		m_canvas = new Canvas(m_bitmap);
		m_background = getBitmapFromAsset(getContext(), "jumping.jpg");
		if(m_background != null)
			m_canvas.drawBitmap(m_background, 0,0, new Paint());
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		
		if(m_canvas == null)
		{
			initBitmap(canvas.getWidth(), canvas.getHeight());
		}
		canvas.drawBitmap(m_bitmap, 0, 0, null);
		if(m_currentObject != null)
			m_currentObject.draw(canvas);
		
		if(_showTouches) drawTouchPoints(canvas);

		if(_showFingersInAir &&  _dataFromServer !=null) drawFingersInAir(canvas);
			
		if(_showAirGestures) _airTouchRecognizer.drawGesture(canvas, g_gestureBrushes);
			
		if(_showDebugText) drawDebugText(canvas);
	}
	
	private void undo()
	{
		if(m_bitmapHistory.size() > 0)
		{
			m_bitmap = m_bitmapHistory.removeLast();
			m_canvas = new Canvas(m_bitmap);
			postInvalidate();
		}
	}

	private void toggleDebug()
	{
		_showAirGestures = !_showAirGestures;
		_showDebugText = !_showDebugText;
	}
	
	public void volumePressed()
	{
		toggleDebug();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		m_beforeTouchRecognizer.setScreenHeight(h);
		m_beforeTouchRecognizer.setScreenWidth(w);
		
		m_betweenTouchRecognizer.setScreenHeight(h);
		m_betweenTouchRecognizer.setScreenWidth(w);
	}
	

}
