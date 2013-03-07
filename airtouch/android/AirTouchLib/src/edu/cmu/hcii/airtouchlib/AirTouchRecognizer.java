package edu.cmu.hcii.airtouchlib;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import lx.interaction.dollar.DollarRecognizer;
import lx.interaction.dollar.Point;
import lx.interaction.dollar.Result;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.MotionEvent;
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
					Log.v(LOG_TAG, "key " + data.fingers[i].id + " not found, adding new linked list");
					m_rollingBuffer.put(data.fingers[i].id, new LinkedList<PMDFinger>());
				}
				LinkedList<PMDFinger> lst = m_rollingBuffer.get(data.fingers[i].id);
				if(lst.size() > 0)
				{
					PMDFinger top = lst.peek();
					while(currentTime - top.timestamp > m_bufferDurationMs && lst.size() > 0)
					{
						// Log.v(LOG_TAG, "updateRecentPoints: popping, dt is " + (currentTime - top.timestamp) + " top timestamp is " + top.timestamp);
						top = lst.pop();
					}	
				}
				lst.add(new PMDFinger(data.fingers[i]));
			}			
		}
		if(m_airTouchType == AirTouchType.AFTER_TOUCH && System.currentTimeMillis() - m_lastTouchUpMs > AFTER_TOUCH_TIMEOUT_MS && m_gestureBuffer.isEmpty())
		{
			copyRollingToGestureBuffer();
		}
	}
	
	public void clear()
	{
		Log.v(LOG_TAG, "clearing rolling buffer");
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

	public void onTouchDown(MotionEvent e)
	{
		long now = System.currentTimeMillis();
		switch(m_airTouchType){
		case AFTER_TOUCH:
			clearGestureBuffer();
			break;
		case BEFORE_TOUCH:
			copyRollingToGestureBuffer();
			break;
		case BETWEEN_TOUCHES:
			clearGestureBuffer();
			long dt =now - m_lastTouchUpMs; 
			if(dt < BETWEEN_TOUCH_TIMEOUT_MS){
				copyRollingToGestureBuffer();
			} else
			{
				Log.v(LOG_TAG, "between touch rejected, duration is " + dt);
			}
			break;
		}
	}
	
	public void onTouchUp(MotionEvent e)
	{
		clear();
		m_lastTouchUpMs = System.currentTimeMillis();
		
		
	}
	
	private void clearGestureBuffer()
	{
		synchronized(m_bufferLock)
		{
			m_gestureBuffer.clear();
			
		}
		
	}
	protected abstract void clearGestureData();
		
	
	private void copyRollingToGestureBuffer()
	{
		clearGestureBuffer();
		synchronized(m_bufferLock)
		{
			Log.v(LOG_TAG, "copying rolling points to gesture buffer, rolling size is " + m_rollingBuffer.entrySet().size());
			m_gestureBuffer.clear();
			for (Entry<Integer, LinkedList<PMDFinger>> path : m_rollingBuffer.entrySet()) 
			{
				
				m_gestureBuffer.put(path.getKey(), new LinkedList<PMDFinger>(path.getValue()));
			}
		}
		recognize();
	}
	
	
	/** 
	 * override for custome gestures
	 */
	protected abstract void recognize();
	
	

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
