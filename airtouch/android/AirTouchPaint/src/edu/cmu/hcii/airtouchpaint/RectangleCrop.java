package edu.cmu.hcii.airtouchpaint;

import edu.cmu.hcii.airtouchpaint.PathCrop.CropState;
import lx.interaction.dollar.Point;
import android.graphics.RectF;
import android.graphics.Path.Direction;
import android.view.MotionEvent;

public class RectangleCrop extends PathCrop {
	
	RectF m_rect = new RectF();
	Point m_p1 = new Point(0,0);
	Point m_p2 = new Point(0,0);
	
	private void resize()
	{
		m_rect.left = (float)Math.min(m_p1.X, m_p2.X);
		m_rect.right = (float)Math.max(m_p1.X, m_p2.X);
		m_rect.top = (float)Math.min(m_p1.Y, m_p2.Y);
		m_rect.bottom = (float)Math.max(m_p1.Y, m_p2.Y);
		m_clipPath.reset();
		m_clipPath.addRect(m_rect, Direction.CCW);
	}
	
	public RectangleCrop(AirTouchPaintView parent, float x, float y, float f, float g)
	{
		super(parent);
		m_p1.X = x;
		m_p1.Y = y;
		m_p2.X = f;
		m_p2.Y = g;
		resize();
		m_state = CropState.Resizing;
	}
	

	@Override
	public void onTouchMove(MotionEvent e) {
		if(m_state == CropState.Resizing)
		{
			m_p2.X = e.getX();
			m_p2.Y = e.getY();
			resize();
		} else if(m_state == CropState.Moving)
		{
			m_xTranslate =  m_startMoveLocation.X + e.getX() - m_startMovePoint.X;
			m_yTranslate = m_startMoveLocation.Y + e.getY() - m_startMovePoint.Y;
		}
	}
	
	@Override
	public void onTouchUp(MotionEvent e) {
		if(m_state == CropState.Canceling)
		{
			m_parent.commitObject();
		}
	}
}
