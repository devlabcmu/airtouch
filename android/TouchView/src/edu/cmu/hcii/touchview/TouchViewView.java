package edu.cmu.hcii.touchview;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.View;

public class TouchViewView extends View {

	static final int TOUCH_SCALE = 1;
	static final int TEXT_SIZE = 24;
	
	static Paint _textPaint;
	static Paint _touchPaint;
	Map<Integer, PointerInfo> _touchMap = new HashMap<Integer, PointerInfo>();
	Object _touchMapLock = new Object();
	RectF _touchDrawRect = new RectF();
	
	static {
		_textPaint = new Paint();
		_textPaint.setTextSize(TEXT_SIZE);
		_textPaint.setColor(Color.BLACK);
		
		_touchPaint = new Paint();
		_touchPaint.setColor(Color.RED);
		
	}
	public TouchViewView(Context context) {
		super(context);
	}

	public TouchViewView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TouchViewView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		/*
		 * Log this touch event 
		 */
		String names[] = { "DOWN" , "UP" , "MOVE" , "CANCEL" , "OUTSIDE", 
				"POINTER_DOWN" , "POINTER_UP" , "7?" , "8?" , "9?" };
		StringBuilder sb = new StringBuilder();
		int action = event.getAction();
		int actionCode = action & MotionEvent.ACTION_MASK;

		sb.append("OnTouchEvent ACTION_" ).append(names[actionCode]);

		sb.append("[" );

		for (int i = 0; i < event.getPointerCount(); i++) {
			sb.append("#" ).append(i);
			sb.append("(pid " ).append(event.getPointerId(i));
			sb.append(")=" ).append((int) event.getX(i));
			sb.append("," ).append((int) event.getY(i));
			if (i + 1 < event.getPointerCount())
				sb.append(";" );
		}
		sb.append("]" );

		Log.v(TouchViewActivity.TAG, sb.toString());
		
		if(actionCode == MotionEvent.ACTION_DOWN || actionCode == MotionEvent.ACTION_MOVE || actionCode == MotionEvent.ACTION_POINTER_DOWN)
			onPointerUpdated(event);
		else if (actionCode == MotionEvent.ACTION_POINTER_UP || actionCode == MotionEvent.ACTION_UP)
			onPointerLost(event);
		
		// update the UI
		invalidate();
		
		// event has been handled
		return true;
	}
	
	private void onPointerUpdated(MotionEvent e)
	{
		synchronized(_touchMapLock){
			for (int i = 0; i < e.getPointerCount(); i++) {
				int id = e.getPointerId(i);
				if(!_touchMap.containsKey(id)){
					_touchMap.put(id, new PointerInfo(id, i));
				}
				PointerInfo pointer = _touchMap.get(id);
				pointer._index = i;
				// update pointer coordinates
				e.getPointerCoords(i, pointer._coords);
				// update pointer properties
				e.getPointerProperties(i, pointer._properties);
			}	
		}
		
	}
	
	private void onPointerLost(MotionEvent e)
	{
		synchronized(_touchMapLock){
			for (int i = 0; i < e.getPointerCount(); i++) {
				int id = e.getPointerId(i);
				Log.v(TouchViewActivity.TAG, "lost " + id);
				if(_touchMap.containsKey(id))
					_touchMap.remove(id);
			}			
		}

	}
	
	/*
	 * 				canvas.save();

				canvas.translate(tp.x, tp.y);
				canvas.rotate(tp.orientation+90);
				paint = paintBrushes.get(tp.touchKind);
				if(paint == null) paint = defaultPaintBrush;
				touchDrawRect.set(
						-tp.touchMajor*TOUCH_SCALE, -tp.touchMinor*TOUCH_SCALE,
						tp.touchMajor*TOUCH_SCALE, tp.touchMinor*TOUCH_SCALE);
				canvas.drawOval(touchDrawRect, paint);
				
				canvas.restore();
	 * (non-Javadoc)
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 */
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawARGB(255, 255, 255, 255);
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("num touches: %d\n", _touchMap.size()));
		
		
		
		synchronized(_touchMapLock){
			for (PointerInfo p : _touchMap.values()) {
				canvas.save();
				canvas.translate(p._coords.x, p._coords.y);
				canvas.rotate(p._coords.orientation + 90);
				_touchDrawRect.set(-p._coords.touchMajor * TOUCH_SCALE, -p._coords.touchMinor*TOUCH_SCALE,
						p._coords.touchMajor * TOUCH_SCALE, p._coords.touchMinor * TOUCH_SCALE);
				canvas.drawOval(_touchDrawRect, _touchPaint);
				canvas.restore();
				
				sb.append(String.format("[%d] %.2f %.2f %.2f %.2f\n", p._id, p._coords.x, p._coords.y, p._coords.touchMajor, p._coords.touchMinor));
			}	
		}
		
		// draw multiline output
		String[] lines = sb.toString().split("\n");
		int startY = TEXT_SIZE;
		for (int i = 0; i < lines.length; i++) {
			canvas.drawText(lines[i],10,  startY + TEXT_SIZE * 1.21f * i, _textPaint);	
		}
		
	}

	class PointerInfo
	{
		PointerCoords _coords = new PointerCoords();
		int _id;
		int _index;
		PointerProperties _properties = new PointerProperties();
		public PointerInfo(int id, int index)
		{
			_index = index;
			_id = id;
		}
		
	}
}
