/**
 * 
 */
package edu.cmu.hcii.hovertest;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnHoverListener;

/**
 * @author brx
 *
 */
public class HoverView extends View implements OnHoverListener {

	private static final int HOVER_EXIT_DELAY = 50; // ms to wait after HOVER_EXIT for a touch event
	private static final int HOVER_ENTER_DELAY = 250; // ms to wait after ACTION_UP for a HOVER_ENTER

	private boolean hovering;
	private PointF hoverPos = new PointF();
	private List<PointF> hoverTrace = new ArrayList<PointF>();
	private float[] drawnTrace;

	private long lastTouchUp;
	private long lastHoverExit;

	public HoverView(Context context) {
		this(context, null);
	}

	public HoverView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HoverView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		setOnHoverListener(this);
	}

	private static Paint hoverCirclePaint = new Paint() {{
		setColor(Color.RED);
	}};
	private static Paint drawnTracePaint = new Paint() {{
		setColor(Color.BLACK);
		setStyle(Style.STROKE);
		setStrokeWidth(5.0f);
	}};
	private static Paint hoverTracePaint = new Paint() {{
		setColor(Color.MAGENTA);
		setStyle(Style.STROKE);
		setStrokeWidth(5.0f);
	}};

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		canvas.drawColor(Color.WHITE);

		if(hovering) {
			canvas.drawCircle(hoverPos.x, hoverPos.y, 30, hoverCirclePaint);
		}
		if(drawnTrace != null) {
			canvas.drawLines(drawnTrace, drawnTracePaint);
		}
		for(int i=0; i<hoverTrace.size()-1; i++) {
			canvas.drawLine(
					hoverTrace.get(i).x, hoverTrace.get(i).y,
					hoverTrace.get(i+1).x, hoverTrace.get(i+1).y,
					hoverTracePaint);
		}
	}

	private Runnable hoverLeave = new Runnable() {
		@Override
		public void run() {
			/* Hover left the screen with no subsequent touch down event. */
		}
	};

	@Override
	public boolean onHover(View v, MotionEvent event) {
		switch(event.getAction()) {
		case MotionEvent.ACTION_HOVER_ENTER:
			if(lastTouchUp < event.getEventTime() + HOVER_ENTER_DELAY) {
				/* Came up from touch */
			}

			hovering = true;
			hoverPos.set(event.getX(), event.getY());
			hoverTrace.clear();
			hoverTrace.add(new PointF(hoverPos.x, hoverPos.y));
			invalidate();
			return true;
		case MotionEvent.ACTION_HOVER_EXIT:
			lastHoverExit = SystemClock.uptimeMillis();
			hovering = false;
			invalidate();
			getHandler().postDelayed(hoverLeave, HOVER_EXIT_DELAY);
			return true;
		case MotionEvent.ACTION_HOVER_MOVE:
			hoverPos.set(event.getX(), event.getY());
			hoverTrace.add(new PointF(hoverPos.x, hoverPos.y));
			invalidate();
			return true;
		default:
			return false;
		}
	}

	public boolean onTouchEvent(MotionEvent event) {
		switch(event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			getHandler().removeCallbacks(hoverLeave);
			if(lastHoverExit < event.getEventTime() + HOVER_EXIT_DELAY) {
				/* Came in from hover */
				drawnTrace = new float[(hoverTrace.size()-1) * 4];
				for(int i=0; i<hoverTrace.size()-1; i++) {
					drawnTrace[i*4 + 0] = hoverTrace.get(i).x;
					drawnTrace[i*4 + 1] = hoverTrace.get(i).y;
					drawnTrace[i*4 + 2] = hoverTrace.get(i+1).x;
					drawnTrace[i*4 + 3] = hoverTrace.get(i+1).y;
				}
				invalidate();
			}
			/* fall through */
		case MotionEvent.ACTION_POINTER_DOWN:
			return true;

		case MotionEvent.ACTION_MOVE:
			return true;

		case MotionEvent.ACTION_UP:
			lastTouchUp = SystemClock.uptimeMillis();
			/* fall through */
		case MotionEvent.ACTION_POINTER_UP:
			return true;
		}
		return super.onTouchEvent(event);
	}
}
