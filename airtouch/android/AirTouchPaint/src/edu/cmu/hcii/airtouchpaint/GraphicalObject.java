package edu.cmu.hcii.airtouchpaint;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public abstract class GraphicalObject {
	static Paint g_defaultPaint;
	static{
		g_defaultPaint = new Paint();
		g_defaultPaint.setStyle(Style.STROKE);
		g_defaultPaint.setStrokeJoin(Join.ROUND);
		g_defaultPaint.setColor(Color.BLACK);
		g_defaultPaint.setStrokeWidth(10.0f);
	}
	protected Paint m_paint = g_defaultPaint;

	public abstract void draw(Canvas c);
	protected AirTouchPaintView m_parent;
	public abstract void onTouchDown(MotionEvent e);
	public abstract void onTouchMove(MotionEvent e);

}
