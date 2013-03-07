package edu.cmu.hcii.dollarrecognizertest;

import java.util.Vector;

import lx.interaction.dollar.DollarRecognizer;
import lx.interaction.dollar.Point;
import lx.interaction.dollar.Result;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DollarRecognizerView extends View {

	public DollarRecognizerView(Context context) {
		super(context);
	}

	public DollarRecognizerView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DollarRecognizerView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	// assuming one finger
	Vector<Point> _touchPoints = new Vector<Point>();
	Result _result;
	static Paint textPaint;
	static Paint touchPaint;
	Path _touchPath = new Path();
	
	static{
		textPaint = new Paint();
		textPaint.setColor(Color.BLACK);
		textPaint.setTextSize(72);
		textPaint.setStrokeJoin(Join.ROUND);
		
		touchPaint = new Paint();
		touchPaint.setColor(Color.GREEN);
		touchPaint.setStyle(Style.STROKE);
		touchPaint.setStrokeMiter(5);
		touchPaint.setStrokeJoin(Join.ROUND);
		touchPaint.setStrokeWidth(5);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawColor(Color.WHITE);
		if(_result != null)
			canvas.drawText(String.format("%s: %.2f",_result.Name, _result.Score), 10, 60, textPaint);
		else
			canvas.drawText("result is null", 10, 60, textPaint);
		
		canvas.drawPath(_touchPath, touchPaint);
		
		super.onDraw(canvas);
	}
	
	// todo: test other gesture sets
	DollarRecognizer _dollarRecognizer = new DollarRecognizer(DollarRecognizer.GESTURES_SIMPLE);
	
	private void clearPoints()
	{
		_touchPoints.clear();
		_touchPath.reset();
	}
	
	private void addPoint(double x, double y)
	{
		_touchPoints.add(new Point(x, y));
		_touchPath.lineTo((float)x, (float)y);
	}
	
	private void firstPoint(double x, double y)
	{
		_touchPoints.add(new Point(x, y));
		_touchPath.moveTo((float)x, (float)y);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// assume only one touch point
		switch(event.getAction()){
		case MotionEvent.ACTION_DOWN:
			clearPoints();
			firstPoint(event.getX(), event.getY());
			break;
		case MotionEvent.ACTION_MOVE:
			addPoint(event.getX(), event.getY());
			break;
		case MotionEvent.ACTION_UP:
			_result = _dollarRecognizer.Recognize(_touchPoints);
			break;
		}
		postInvalidate();
		return true;
	}
}
