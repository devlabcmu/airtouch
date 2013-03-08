package edu.cmu.hcii.airtouchpaint;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class Stroke extends GraphicalObject {


	public Stroke(AirTouchPaintView parent) {
		super(parent);
	}

	private Path m_path = new Path();

	@Override
	public void draw(Canvas c) {
		c.drawPath(m_path, m_paint);
	}

	@Override
	public void onTouchMove(MotionEvent e) {
		m_path.lineTo(e.getX(), e.getY());
		m_path.computeBounds(m_boundingBox, true);
		m_boundingBox.left -= m_paint.getStrokeWidth();
		m_boundingBox.top -= m_paint.getStrokeWidth();
		m_boundingBox.right += m_paint.getStrokeWidth();
		m_boundingBox.bottom += m_paint.getStrokeWidth();
		m_parent.damage(m_boundingBox);
	}

	@Override
	public void onTouchDown(MotionEvent e) {
		m_path.reset();
		m_path.moveTo(e.getX(), e.getY());
	}

	

}
