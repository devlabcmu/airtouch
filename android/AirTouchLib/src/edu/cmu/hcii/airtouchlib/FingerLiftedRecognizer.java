package edu.cmu.hcii.airtouchlib;

import java.util.LinkedList;
import java.util.Map.Entry;

import lx.interaction.dollar.Result;
import android.util.Log;

public class FingerLiftedRecognizer extends AirTouchRecognizer {
	public static final float LIFTED_THRESHOLD = 0.05f;
	static final String LOG_TAG="AirTouch.FingerLiftedRecognizer";
	public FingerLiftedRecognizer(long bufferDuration, AirTouchType type) {
		super(bufferDuration, type);
	}

	@Override
	protected void clearGestureData() {
	}

	@Override
	protected void recognize() {
		
		for (Entry<Integer, LinkedList<PMDFinger>> path : m_gestureBuffer.entrySet()) 
		{
			float maxY = 0;
			for (PMDFinger f : path.getValue()) {
				if(f.id >= 0)
				{
					// get max y value
					if(f.y > maxY)
					{
						maxY = f.y;
					}
					// if max y > threshold, add result "FingerLifted"
				}
			}
			Log.i(LOG_TAG, "max Y is " + maxY);
			if(maxY > LIFTED_THRESHOLD)
			{
				m_gestureResults.put(path.getKey(), new Result("fingerlifted", 1.0f, 0));
			}
		}
		
	}

}
