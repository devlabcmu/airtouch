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
	public Path getPath()
	{
		return m_path;
	}
	
	@Override
	public void draw(Canvas c) {
		c.drawPath(m_path, m_paint);
	}

	@Override
	public void onTouchMove(MotionEvent e) {
		m_path.lineTo(e.getX(), e.getY());
	}

	@Override
	public void onTouchDown(MotionEvent e) {
		m_path.reset();
		m_path.moveTo(e.getX(), e.getY());
	}

	@Override
	public void onTouchUp(MotionEvent e) {
		m_parent.commitObject();
	}

	@Override
	public void drawCommit(Canvas c) {
		draw(c);
		
	}

}
