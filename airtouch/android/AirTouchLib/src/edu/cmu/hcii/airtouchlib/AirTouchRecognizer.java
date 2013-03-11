package edu.cmu.hcii.airtouchlib;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import edu.cmu.hcii.airtouchlib.AirTouchPoint.TouchType;
import edu.cmu.hcii.airtouchlib.SendReceiveTask.PMDSendData;

@SuppressLint("UseSparseArrays")
public abstract class AirTouchRecognizer implements PMDDataHandler {

	// Constants
	public static final long BETWEEN_TOUCH_TIMEOUT_MS = 1000;
	public static final long AFTER_TOUCH_TIMEOUT_MS = 1000;
	public static final String LOG_TAG = "AirTouch.AirTouchRecognizer";
	
	public enum AirTouchType
	{
		BEFORE_TOUCH,
		BETWEEN_TOUCHES,
		AFTER_TOUCH
	}
	// Properties
	private AirTouchType m_airTouchType;
	private long m_bufferDurationMs;
	protected double m_screenWidth;
	protected double m_screenHeight;
	
	// Points
	private Map<Integer, LinkedList<PMDFinger>> m_rollingBuffer = new HashMap<Integer, LinkedList<PMDFinger>>();
	protected Map<Integer, LinkedList<PMDFinger>> m_gestureBuffer = new HashMap<Integer, LinkedList<PMDFinger>>();
	Object m_bufferLock = new Object();

	// Drawing
	Path gesturePath = new Path();
	
	private long m_lastTouchUpMs;
	
	public AirTouchRecognizer(long bufferDuration, AirTouchType type) {
		m_bufferDurationMs = bufferDuration;
		m_airTouchType = type;
	}
		
	public void updateRecentPoints(PMDSendData data)
	{
		long currentTime = System.currentTimeMillis();
		synchronized(m_bufferLock)
		{
			for (int i = 0; i < data.fingers.length; i++) {
				if(data.fingers[i].id < 0) continue;
				if(!m_rollingBuffer.containsKey(data.fingers[i].id))
				{
//					Log.i(LOG_TAG, "key " + data.fingers[i].id + " not found, adding new linked list");
					m_rollingBuffer.put(data.fingers[i].id, new LinkedList<PMDFinger>());
				}
				LinkedList<PMDFinger> lst = m_rollingBuffer.get(data.fingers[i].id);
				if(lst.size() > 0)
				{
					PMDFinger top = lst.peek();
					while(currentTime - top.timestamp > m_bufferDurationMs && lst.size() > 0)
					{
						// Log.i(LOG_TAG, "updateRecentPoints: popping, dt is " + (currentTime - top.timestamp) + " top timestamp is " + top.timestamp);
						top = lst.pop();
					}	
				}
				lst.add(new PMDFinger(data.fingers[i]));
			}			
		}
		if(m_airTouchType == AirTouchType.AFTER_TOUCH && System.currentTimeMillis() - m_lastTouchUpMs > AFTER_TOUCH_TIMEOUT_MS && m_gestureBuffer.isEmpty() && !m_fingerDown)
		{
			Log.i(LOG_TAG, "after touch recognizing gesture" );
			copyRollingToGestureBuffer();
		}
	}
	
	public void clear()
	{
//		Log.i(LOG_TAG, "clearing rolling buffer");
		m_rollingBuffer.clear();
		clearGestureBuffer();
		
	}
	
	@Override
	public void newPMDData(PMDSendData data) {
		updateRecentPoints(data);
	}

	@Override
	public void onSendReceiveTaskFailed(String message) {

	}

	private boolean m_fingerDown = false;
	public void onTouchDown(MotionEvent e)
	{
		m_fingerDown = true;
		long now = System.currentTimeMillis();
		switch(m_airTouchType){
		case AFTER_TOUCH:
			clearGestureBuffer();
			break;
		case BEFORE_TOUCH:
			copyRollingToGestureBuffer();
			break;
		case BETWEEN_TOUCHES:
			long dt =now - m_lastTouchUpMs; 
			if(dt < BETWEEN_TOUCH_TIMEOUT_MS){
				copyRollingToGestureBuffer();
			} else
			{
				clearGestureBuffer();
				Log.i(LOG_TAG, "between touch rejected, duration is " + dt);
			}
			break;
		}
	}
	
	public void onTouchUp(MotionEvent e)
	{
		m_fingerDown = false;
		m_lastTouchUpMs = System.currentTimeMillis();
		
	}
	
	protected void clearGestureBuffer()
	{
		synchronized(m_bufferLock)
		{
			m_gestureBuffer.clear();
			
		}
		clearGestureData();
		
	}
	protected abstract void clearGestureData();
		
	
	private void copyRollingToGestureBuffer()
	{
		synchronized(m_bufferLock)
		{
//			Log.i(LOG_TAG, "copying rolling points to gesture buffer, rolling size is " + m_rollingBuffer.entrySet().size());
			clearGestureBuffer();
			for (Entry<Integer, LinkedList<PMDFinger>> path : m_rollingBuffer.entrySet()) 
			{
//				Log.i(LOG_TAG, "number point in buffer is  " + path.getValue().size());
				m_gestureBuffer.put(path.getKey(), new LinkedList<PMDFinger>(path.getValue()));
			}
		}
		recognize();
	}
	
	
	/** 
	 * override for custome gestures
	 */
	protected abstract void recognize();
	
	public PMDFinger phoneToScreen(PMDFinger f)
	{
		PMDFinger result = new PMDFinger(f);
		float realDepth = 0.1f;
		result.x = f.x  * (float)m_screenWidth;
		result.y = f.z  * (float)m_screenHeight;
		result.z = f.y / realDepth;
		return result;
	}
	
	/**
	 * Draw debug info for the gesture
	 */
	public void drawGesture(Canvas canvas, Map<AirTouchPoint.TouchType, Paint> paintBrushes)
	{
		for (Entry<Integer, LinkedList<PMDFinger>> paths : m_gestureBuffer.entrySet()) {
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

	public long getBufferDurationMs() {
		return m_bufferDurationMs;
	}

	public void setBufferDurationMs(long bufferDurationMs) {
		m_bufferDurationMs = bufferDurationMs;
	}

	public  Map<Integer, LinkedList<PMDFinger>> getAllGestureBuffers()
	{
		return m_gestureBuffer;
	}

	public AirTouchType getAirTouchType() {
		return m_airTouchType;
	}

	public void setAirTouchType(AirTouchType airTouchType) {
		m_airTouchType = airTouchType;
	}

	public double getScreenWidth() {
		return m_screenWidth;
	}

	public void setScreenWidth(double screenWidth) {
		m_screenWidth = screenWidth;
	}

	public double getScreenHeight() {
		return m_screenHeight;
	}

	public void setScreenHeight(double screenHeight) {
		m_screenHeight = screenHeight;
	}

	
}
