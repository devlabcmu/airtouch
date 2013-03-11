package edu.cmu.hcii.airtouchlib;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import lx.interaction.dollar.DollarRecognizer;
import lx.interaction.dollar.Point;
import lx.interaction.dollar.Rectangle;
import lx.interaction.dollar.Result;
import lx.interaction.dollar.Utils;
import android.os.Environment;
import android.util.Log;

public class AirTouchDollarRecognizer extends AirTouchRecognizer {
	static final String LOG_TAG="AirTouch.AirTouchDollarRecognizer";
	static String GESTURES_PATH="/atgest";
	
	static double MIN_GESTURE_AREA = 5000;
	// Gestures
	DollarRecognizer m_dollarRecognizer;
	Map<Integer, Vector<Point>> m_dollarPoints = new HashMap<Integer, Vector<Point>>();
	Map<Integer, Result> m_gestureResults = new HashMap<Integer, Result>();
	
	static File g_gestureFileRoot;
	
	public AirTouchDollarRecognizer(long bufferDuration, AirTouchType type) {
		super(bufferDuration, type);
		 m_dollarRecognizer= new DollarRecognizer();
	}
	
	public AirTouchDollarRecognizer(long bufferDuration, AirTouchType type, int gestureSet) {
		super(bufferDuration, type);
		Log.i(LOG_TAG, "Using hard coded gesture set");
		 m_dollarRecognizer= new DollarRecognizer(gestureSet);
	}
	
	public void loadGestureSet(String gestureSetFolder)
	{
		Log.i(LOG_TAG, "Loading gestures from folder " + gestureSetFolder);
		// get external storage directory
		// get path to directory, creating folders if they do not exist
		loadGestureRoot();
		
		
		// get directory
		File gestureSetRoot = new File(g_gestureFileRoot.getAbsolutePath(), gestureSetFolder);
		if(!gestureSetRoot.exists())
		{
			Log.e(LOG_TAG, "gesture set folder " + gestureSetRoot.getAbsolutePath() + " doesn't exist!");
		} else if (!gestureSetRoot.isDirectory()){
			Log.e(LOG_TAG, "gesture set " + gestureSetRoot.getAbsolutePath() + " is not a directory!");
		} else
		{
			m_dollarRecognizer.loadTemplates(gestureSetRoot.listFiles());
			Log.i(LOG_TAG, "loaded templates " + gestureSetRoot.getAbsolutePath());
		}
	}
	
	final String NEW_FILE_ROOT="saved";
	public void saveBufferToGesture(String name)
	{
		loadGestureRoot();
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMddHHmmss");
	    Date now = new Date();
	    String strDate = sdfDate.format(now);
		
	    File newFileRoot = new File(g_gestureFileRoot.getAbsolutePath(), NEW_FILE_ROOT);
	    if(!newFileRoot.exists())
	    {
	    	Log.i(LOG_TAG, "Creating directory " + newFileRoot);
	    	newFileRoot.mkdir();
	    }
	    Log.i(LOG_TAG, "saving gesture " + name + " to path " + newFileRoot);
		int i = 0;
		for (Vector<Point> pts : m_dollarPoints.values()) {
			Log.i(LOG_TAG, "saving gesture " + i);
			String filename =   name + sdfDate.format(now) + "_"+i+".xml";
			Utils.writeGestureToFile(pts, name, new File(newFileRoot.getAbsolutePath(), filename));
			i++;
		}
	}
	
	/**
	 * 
	 * @return path to root of all gesture sets
	 */
	private static void loadGestureRoot()
	{
		if(g_gestureFileRoot != null) return;
		Log.i(LOG_TAG, "initializing gesture root");
		// use /sdcard dir for simplicity
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + GESTURES_PATH;
		g_gestureFileRoot = new File(path);
		if(!g_gestureFileRoot.exists())
			g_gestureFileRoot.mkdir();
	}
	
	@Override
	protected void recognize() {
		for (Entry<Integer, LinkedList<PMDFinger>> path : m_gestureBuffer.entrySet()) 
		{
			// to do: perform the recognition in screen space???
			Vector<Point> newpts = new Vector<Point>();

			for (PMDFinger f : path.getValue()) {
				if(f.id >= 0)
				//					Log.i(LOG_TAG, "width is " + m_screenWidth + " height " + m_screenHeight + " adding for dollar: " + f.x  + ", " + f.z );
					newpts.add(new Point(f.x * m_screenWidth, f.z * m_screenHeight));
			}
			Rectangle r = Utils.BoundingBox(newpts);
			if(r.Width * r.Height > MIN_GESTURE_AREA){
				m_gestureResults.put(path.getKey(), m_dollarRecognizer.Recognize(newpts));
				m_dollarPoints.put(path.getKey(), newpts);
			}
		}
		Log.i(LOG_TAG, "recognizing");
	}
	
	public DollarRecognizer getDollarRecognizer()
	{
		return m_dollarRecognizer;
	}

	public Map<Integer, Vector<Point>> getDollarRecognizerInputs()
	{
		return m_dollarPoints;
	}
	
	@Override
	protected void clearGestureData() {
		Log.i(LOG_TAG, "clearing gesture data");
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
