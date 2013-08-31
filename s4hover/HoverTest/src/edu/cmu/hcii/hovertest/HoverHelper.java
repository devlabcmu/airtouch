package edu.cmu.hcii.hovertest;

import java.lang.ref.WeakReference;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnHoverListener;

public class HoverHelper implements OnHoverListener {

	private OnHoverListener listener;
	private Handler handler;
	private long lastTouchUp;

	private static final int MSG_HOVER_EXIT = 1;

	static private class HoverHandler extends Handler {
		WeakReference<OnHoverListener> listener;
		public HoverHandler(OnHoverListener listener) {
			this.listener = new WeakReference<OnHoverListener>(listener);
		}

		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_HOVER_EXIT: {
				Bundle bundle = msg.getData();
				MotionEvent event = (MotionEvent)bundle.getParcelable("MotionEvent");
				OnHoverListener listener = this.listener.get();
				if(listener != null)
					listener.onHover((View)listener, event);
				break;
			}
			default:
			}
		}
	}

	public HoverHelper(OnHoverListener listener) {
		this.listener = listener;
		this.handler = new HoverHandler(listener);
	}

	@Override
	public boolean onHover(View v, MotionEvent event) {
		switch(event.getAction()) {
		case MotionEvent.ACTION_HOVER_ENTER:
			if(event.getEventTime() > lastTouchUp + 250) {
				/* only fire event if a pointer hasn't gone up recently */
				return listener.onHover(v, event);
			}
			return true;
		case MotionEvent.ACTION_HOVER_EXIT: {
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putParcelable("MotionEvent", event);
			msg.setData(bundle);
			msg.what = MSG_HOVER_EXIT;
			handler.sendMessageAtTime(msg, event.getEventTime() + 50);
			return true;
		}
		case MotionEvent.ACTION_HOVER_MOVE:
			/* always fire moves */
		default:
			return listener.onHover(v, event);
		}
	}

	public void onTouchEvent(MotionEvent event) {
		switch(event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			handler.removeMessages(MSG_HOVER_EXIT);
			break;
		case MotionEvent.ACTION_UP:
			lastTouchUp = SystemClock.uptimeMillis();
			break;
		}
	}
}
