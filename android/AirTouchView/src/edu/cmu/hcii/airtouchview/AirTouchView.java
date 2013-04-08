package edu.cmu.hcii.airtouchview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import edu.cmu.hcii.airtouchlib.AirTouchViewBase;


@SuppressLint("UseSparseArrays")
public class AirTouchView extends AirTouchViewBase{
	static String TAG = "AirTouch.AirTouchView";
	
	Paint m_ripplePaint = new Paint();
	PointF m_touchPoint = new PointF();
	float m_rippleAlpa = 0;
	float m_touchRadius = 50;
	
	public AirTouchView(Context context) {
		super(context);
	}

	public AirTouchView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirTouchView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onTouchDown(MotionEvent event) {
		// TODO Auto-generated method stub
		super.onTouchDown(event);
		m_touchPoint.x = event.getX();
		m_touchPoint.y = event.getY();
		m_rippleAlpa = 255;
		m_touchRadius = 50;
		m_factor = 1.2f;
	}
	
	float m_factor = 1.2f;
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		
		if(m_rippleAlpa > 1){
			m_ripplePaint.setAlpha((int)m_rippleAlpa);
			canvas.drawCircle(m_touchPoint.x, m_touchPoint.y, m_touchRadius, m_ripplePaint);
			m_touchRadius *= m_factor;
			if(m_factor > 1.05){
				m_factor *= 0.99f;
			}
			m_rippleAlpa *= 0.85;
		}
		
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		
		m_errorText = null;
		
		m_ripplePaint.setColor(Color.RED);
		m_ripplePaint.setStyle(Style.STROKE);
		m_ripplePaint.setStrokeWidth(20);
	}
}
