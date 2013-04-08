package edu.cmu.hcii.airtouchpaint;

import lx.interaction.dollar.Point;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.MotionEvent;

public abstract class PathCrop extends GraphicalObject {
	public enum CropState{Start, Resizing, Moving, Canceling};
	
	protected CropState m_state = CropState.Start;
	
	static Paint g_paint;
	static Paint g_clearPaint;
	static
	{
		g_paint = new Paint();
		g_paint.setStyle(Style.STROKE);
		g_paint.setColor(Color.WHITE);
		g_paint.setStrokeWidth(10);
		
		g_clearPaint = new Paint();
		g_clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
	}
	protected Path m_clipPath = new Path();
	protected Bitmap m_background;
	protected Bitmap m_stampingBitmap;
	protected Canvas m_canvas;
	
	protected Point m_startMovePoint;
	protected Point m_startMoveLocation;
	protected double m_xTranslate = 0;
	protected double m_yTranslate = 0;
	public PathCrop(AirTouchPaintView parent) {
		super(parent);
		m_background = parent.getBackgroundBitmap();
		m_stampingBitmap = Bitmap.createBitmap(parent.getWidth(), parent.getHeight(), Bitmap.Config.ARGB_8888);
		m_canvas = new Canvas(m_stampingBitmap);
	}
	
	@Override
	public void draw(Canvas c) {
		c.drawARGB(100, 0, 0, 0);
		m_canvas.save();
		m_canvas.drawRect(0,0, m_canvas.getWidth(), m_canvas.getHeight(), g_clearPaint);
		m_canvas.clipPath(m_clipPath);
		m_canvas.drawBitmap(m_background, 0, 0, g_paint);
		m_canvas.restore();
		
		c.save();
		c.translate((float)m_xTranslate, (float)m_yTranslate);
		c.drawBitmap(m_stampingBitmap, 0, 0, g_paint);
		c.drawPath(m_clipPath, g_paint);
		c.restore();
		
	}

	public void drawCommit(Canvas c)
	{
		m_canvas.save();
		m_canvas.drawRect(0,0, m_canvas.getWidth(), m_canvas.getHeight(), g_clearPaint);
		m_canvas.clipPath(m_clipPath);
		m_canvas.drawBitmap(m_background, 0, 0, g_paint);
		m_canvas.restore();
		
		c.save();
		c.translate((float)m_xTranslate, (float)m_yTranslate);
		c.drawBitmap(m_stampingBitmap, 0, 0, g_paint);
		c.restore();
	}

	@Override
	public void onTouchDown(MotionEvent e) {
		RectF boundingRect = new RectF();
		m_clipPath.computeBounds(boundingRect, true);
		if(boundingRect.contains(e.getX() - (float)m_xTranslate, e.getY() - (float)m_yTranslate))
		{
			m_state = CropState.Moving;
			m_startMovePoint = new Point(e.getX(), e.getY());
			m_startMoveLocation = new Point(m_xTranslate, m_yTranslate);
		} else
		{
			m_state = CropState.Canceling;
		}
	}

	@Override
	public void onTouchMove(MotionEvent e) {
		if(m_state == CropState.Moving)
		{
			m_xTranslate = m_startMoveLocation.X + e.getX() - m_startMovePoint.X;
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
