package edu.cmu.hcii.airtouchpaint;

import lx.interaction.dollar.Point;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;

public class Rectangle extends GraphicalObject {
	Point m_p1 = new Point(0,0);
	Point m_p2 = new Point(0,0);
	RectF m_rect = new RectF();
	public Rectangle(AirTouchPaintView parent) {
		super(parent);
	}

	public Rectangle(AirTouchPaintView parent, float x1, float y1, float x2, float y2) {
		super(parent);
		m_p1.X = x1;
		m_p1.Y = y1;
		m_p2.X = x2;
		m_p2.Y = y2;
		resize();
	}
	
	@Override
	public void draw(Canvas c) {
		c.drawRect(m_rect, m_paint);
	}
	
	private void resize()
	{
		m_rect.left = (float)Math.min(m_p1.X, m_p2.X);
		m_rect.right = (float)Math.max(m_p1.X, m_p2.X);
		m_rect.top = (float)Math.min(m_p1.Y, m_p2.Y);
		m_rect.bottom = (float)Math.max(m_p1.Y, m_p2.Y);
		updateBounds();
	}

	@Override
	public void onTouchDown(MotionEvent e) {
		m_p1.X = e.getX();
		m_p1.Y = e.getY();
		m_p2.X = e.getX();
		m_p2.Y = e.getY();		
		resize();
		
	}

	
	@Override
	public void onTouchMove(MotionEvent e) {
		m_p2.X = e.getX();
		m_p2.Y = e.getY();
		resize();
	}
	
	private void updateBounds()
	{
		m_parent.damage(m_boundingBox);
		m_boundingBox.bottom = m_rect.bottom;
		m_boundingBox.left = m_rect.left;
		m_boundingBox.right = m_rect.right;
		m_boundingBox.top = m_rect.top;
		m_boundingBox.left -= m_paint.getStrokeWidth();
		m_boundingBox.top -= m_paint.getStrokeWidth();
		m_boundingBox.right += m_paint.getStrokeWidth();
		m_boundingBox.bottom += m_paint.getStrokeWidth();
		m_parent.damage(m_boundingBox);
	}

	@Override
	public RectF getBoundingBox() {
		return m_rect;
	}
	
	

}
