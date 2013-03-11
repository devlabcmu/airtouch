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
import edu.cmu.hcii.airtouchlib.AirTouchRecognizer.AirTouchType;
import edu.cmu.hcii.airtouchlib.AirTouchViewBase;
import edu.cmu.hcii.airtouchlib.PMDDataHandler;
import edu.cmu.hcii.airtouchlib.PMDFinger;
import edu.cmu.hcii.airtouchlib.SendReceiveTask;

public class AirTouchPaintView extends AirTouchViewBase {
	static String LOG_TAG = "AirTouchPaintView";
	protected static Map<AirTouchPoint.TouchType, Paint> gestureBrushes = new HashMap<AirTouchPoint.TouchType, Paint>();
	static Paint shadowPaint;
	static
	{
		Paint paint;

		paint = new Paint();
		paint.setColor(Color.LTGRAY);
		shadowPaint = paint;
		
		paint = new Paint();
		paint.setColor(Color.MAGENTA);
		paint.setStyle(Style.STROKE);
		gestureBrushes.put(AirTouchPoint.TouchType.TOUCH_DOWN, paint);

		paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setStyle(Style.STROKE);
		gestureBrushes.put(AirTouchPoint.TouchType.TOUCH_MOVE, paint);

		paint = new Paint();
		paint.setColor(Color.BLUE);
		paint.setStyle(Style.STROKE);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeWidth(5);
		gestureBrushes.put(AirTouchPoint.TouchType.AIR_MOVE1, paint);

		paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStyle(Style.STROKE);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeWidth(5);
		gestureBrushes.put(AirTouchPoint.TouchType.AIR_MOVE2, paint);

	}
	enum Command {STROKE, RECT, CLEAR};
	static final int UNDO_HISTORY_SIZE = 5;

	
	LinkedList<Bitmap> m_bitmapHistory = new LinkedList<Bitmap>();
	Bitmap m_bitmap;
	Canvas m_canvas;
	
	long m_lastTouchDownMs;
	Point m_lastTouchPoint = new Point(0,0);
	
	Bitmap m_background;
	
	// very simple one-level view
	
	GraphicalObject m_currentObject;
	
	// 
	public AirTouchPaintView(Context context) {
		super(context);
	}

	public AirTouchPaintView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirTouchPaintView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	protected  AirTouchDollarRecognizer _beforeTouchRecognizer = new AirTouchDollarRecognizer(700, AirTouchType.BEFORE_TOUCH);
	protected  AirTouchDollarRecognizer _betweenTouchRecognizer = new AirTouchDollarRecognizer(1000, AirTouchType.BETWEEN_TOUCHES);
	
	@Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		_beforeTouchRecognizer.loadGestureSet("default");
		_betweenTouchRecognizer.loadGestureSet("default");
	}
	
	@Override
	protected void beginReceivingData() {
		final PMDDataHandler me = this;

		// handshake has already happened
		// begin sending and receiving data
		TimerTask task = new TimerTask() {
			public void run() {
				new SendReceiveTask(_connection, true, me, _airTouchRecognizer, _beforeTouchRecognizer,_betweenTouchRecognizer).execute();

			}
		};
		_timer = new Timer();
		_timer.scheduleAtFixedRate(task, 1, 20);
	}
	
	private Command getCommand()
	{
		Map<Integer, Result> gestureResults = _betweenTouchRecognizer.getGestureResults();
		// if either finger has recognized caret
		for (Entry<Integer, Result> result : gestureResults.entrySet()) {
			if(result.getValue().Name.contains("caret") && result.getValue().Score > 0.8) return Command.RECT;
		}
		
		gestureResults = _beforeTouchRecognizer.getGestureResults();
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
		_beforeTouchRecognizer.onTouchDown(event);
		_betweenTouchRecognizer.onTouchDown(event);
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
		_beforeTouchRecognizer.onTouchUp(event);
		_betweenTouchRecognizer.onTouchUp(event);
		if(m_currentObject != null)
		{
			addBitmapToHistory();
			// rasterize the current object
			m_currentObject.draw(m_canvas);
		}
		m_currentObject = null;
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
				shadowPaint.setAlpha((int)(1 - 255 * Math.max(0, Math.min(1, p.z) ) ));

				canvas.drawCircle(p.x, p.y, p.z * 500,shadowPaint);	
				shadowPaint.setAlpha(1);
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
			
		if(_showAirGestures) _airTouchRecognizer.drawGesture(canvas, gestureBrushes);
			
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
		_beforeTouchRecognizer.setScreenHeight(h);
		_beforeTouchRecognizer.setScreenWidth(w);
		
		_betweenTouchRecognizer.setScreenHeight(h);
		_betweenTouchRecognizer.setScreenWidth(w);
	}
	

}
