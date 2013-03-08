package edu.cmu.hcii.airtouchlib;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import lx.interaction.dollar.DollarRecognizer;
import lx.interaction.dollar.Point;
import lx.interaction.dollar.Rectangle;
import lx.interaction.dollar.Result;
import lx.interaction.dollar.Utils;

public class AirTouchDollarRecognizer extends AirTouchRecognizer {
	static double MIN_GESTURE_AREA = 5000;
	// Gestures
	DollarRecognizer m_dollarRecognizer;
	Map<Integer, Vector<Point>> m_dollarPoints = new HashMap<Integer, Vector<Point>>();
	Map<Integer, Result> m_gestureResults = new HashMap<Integer, Result>();
	
	
	public AirTouchDollarRecognizer(long bufferDuration, AirTouchType type) {
		super(bufferDuration, type);
		
		 m_dollarRecognizer= new DollarRecognizer(DollarRecognizer.GESTURES_SIMPLE);
	}

	public AirTouchDollarRecognizer(long bufferDuration, AirTouchType type, List<InputStream> templates) {
		super(bufferDuration, type);
		
		 m_dollarRecognizer= new DollarRecognizer(templates);
	}

	
	@Override
	protected void recognize() {
		clearGestureData();
		for (Entry<Integer, LinkedList<PMDFinger>> path : m_gestureBuffer.entrySet()) 
		{
			// to do: perform the recognition in screen space???
			Vector<Point> newpts = new Vector<Point>();

			for (PMDFinger f : path.getValue()) {
				//					Log.v(LOG_TAG, "width is " + m_screenWidth + " height " + m_screenHeight + " adding for dollar: " + f.x  + ", " + f.z );
				newpts.add(new Point(f.x * m_screenWidth, f.z * m_screenHeight));
			}
			Rectangle r = Utils.BoundingBox(newpts);
			if(r.Width * r.Height > MIN_GESTURE_AREA){
				m_gestureResults.put(path.getKey(), m_dollarRecognizer.Recognize(newpts));
				m_dollarPoints.put(path.getKey(), newpts);
			}
		}
	}

	public Map<Integer, Vector<Point>> getDollarRecognizerInputs()
	{
		return m_dollarPoints;
	}
	
	@Override
	protected void clearGestureData() {
		m_gestureResults.clear();
		m_dollarPoints.clear();
	}
	
	public Map<Integer, Result> getGestureResults()
	{
		return m_gestureResults;
	}
	
	public Map<Integer, Vector<Point>> getDollarPoints()
	{
		return m_dollarPoints;
	}
	

}
