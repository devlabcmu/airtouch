package edu.cmu.hcii.airtouchpaint;

import java.security.acl.LastOwnerException;
import java.util.Stack;

import lx.interaction.dollar.Point;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import edu.cmu.hcii.airtouchlib.AirTouchRecognizer;

public class AirTouchPaintView extends View {
	static Paint g_whitePaint;
	static{
		g_whitePaint = new Paint();
		g_whitePaint.setColor(Color.WHITE);
		g_whitePaint.setStyle(Style.FILL);
	}
	
	Path m_damagePath = new Path();
	RectF m_damageBounds = new RectF();
	
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
				m_currentObject = new Rectangle(this, (float)m_lastTouchPoint.X, (float)m_lastTouchPoint.Y, event.getX(), event.getY());
			} else
			{
				m_currentObject = new Stroke(this);	
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
				m_children.add(m_currentObject);
			m_currentObject = null;
			handled = true;
			break;
		}
		postInvalidate();
		return handled || super.onTouchEvent(event);
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
		canvas.drawRGB(255,  255, 255);		
		canvas.drawBitmap(m_bitmap, 0, 0, null);
		draw();
		
	}
	
	private void draw()
	{
		// very basic for now: just redraw all children
		m_damagePath.computeBounds(m_damageBounds, true);
		
		m_canvas.drawRect(m_damageBounds, g_whitePaint);
		
		// more advanced would be to looka at damaged aread
		// or just draw over if needed (i.e. dont' redraw
		for (GraphicalObject obj : m_children) {
			if(obj != null && RectF.intersects(obj.getBoundingBox(), m_damageBounds)){
				obj.draw(m_canvas);	
			}
		}
		if(m_currentObject != null)
			m_currentObject.draw(m_canvas);
		
		m_damagePath.reset();
	}
	
	public void damage(RectF r)
	{
		m_damagePath.addRect(r, Direction.CCW);
	}
	
	private void undo()
	{
		if(m_children.size() > 0){
			GraphicalObject removed = m_children.pop();
			if(removed == null)
			{
				Log.v("AirTouchPaint", "removed is null! children size is " + m_children.size());
			} else
			{
				damage(new RectF(0,0, m_canvas.getWidth(), m_canvas.getHeight()));
				postInvalidate();
			}
			
			
		}
	}
	
	
	

}
