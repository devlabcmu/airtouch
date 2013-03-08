package edu.cmu.hcii.airtouchpaint;

import java.util.LinkedList;
import java.util.Stack;

import lx.interaction.dollar.Point;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import edu.cmu.hcii.airtouchlib.AirTouchRecognizer;

public class AirTouchPaintView extends View {
	static final int UNDO_HISTORY_SIZE = 5;
	LinkedList<Bitmap> m_bitmapHistory = new LinkedList<Bitmap>();
	Bitmap m_bitmap;
	Canvas m_canvas;
	
	long m_lastTouchDownMs;
	Point m_lastTouchPoint = new Point(0,0);
	
	// very simple one-level view
	Stack<GraphicalObject> m_children = new Stack<GraphicalObject>();
	
	GraphicalObject m_currentObject;
	public AirTouchPaintView(Context context) {
		super(context);
	}

	public AirTouchPaintView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirTouchPaintView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean handled = false;
		long now = System.currentTimeMillis();
		switch(event.getAction())
		{
		case MotionEvent.ACTION_DOWN:
			if(now - m_lastTouchDownMs < AirTouchRecognizer.BETWEEN_TOUCH_TIMEOUT_MS)
			{
				undo();
				m_currentObject = new Rectangle((float)m_lastTouchPoint.X, (float)m_lastTouchPoint.Y, event.getX(), event.getY());
			} else
			{
				m_currentObject = new Stroke();	
				// check if we are supposed to be making a rectangle
				m_currentObject.onTouchDown(event);
			}
			handled = true;
			updateLastPoint(event);
			m_lastTouchDownMs = now;
			break;
		case MotionEvent.ACTION_MOVE:
			if(m_currentObject != null){
				m_currentObject.onTouchMove(event);
				handled = true;
			}
			updateLastPoint(event);
			break;
		case MotionEvent.ACTION_UP:
			if(m_currentObject != null)
			{
				addBitmapToHistory();
				// rasterize the current object
				m_currentObject.draw(m_canvas);
			}

			m_currentObject = null;
			handled = true;
			break;
		}
		postInvalidate();
		return handled || super.onTouchEvent(event);
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
	
	@Override
	protected void onDraw(Canvas canvas) {
		if(m_canvas == null)
		{
			m_bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
			m_canvas = new Canvas(m_bitmap);
			m_canvas.drawRGB(255, 255, 255);
		}
		canvas.drawBitmap(m_bitmap, 0, 0, null);
		if(m_currentObject != null)
			m_currentObject.draw(canvas);
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
}
