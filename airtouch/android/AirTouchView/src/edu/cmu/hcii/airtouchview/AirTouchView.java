package edu.cmu.hcii.airtouchview;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import edu.cmu.hcii.airtouchview.AirTouchPoint.TouchType;


public class AirTouchView extends View {

	// Constants
	static final int TOUCH_SCALE = 50;
	static final int PMD_IMAGE_SIZE = 19800;
	static final int PMD_NUM_COLS = 165;
	static final int PMD_NUM_ROWS = 120;
	static final int PMD_SEND_DATA_SIZE = 79212;
	static final int PMD_FINGER_ONLY_DATA_SIZE = 32;
	static final int PMD_INVALID_DISTANCE = -1000;
	static final boolean OUTPUT_BITS_DEBUG = false;

	static String TAG = "AirTouchViewView";
	
	

	// convenience structs
	public class Point3f
	{
		public float x;
		public float y;
		public float z;
		
	}
	
	public class PMDFinger {
		public int id;
		public float x;
		public float y;
		public float z;
		
	}
	
	// structs from PMD
	public class PMDSendData {
		public PMDFinger[] fingers = new PMDFinger[2]; 
		public float[] buffer = new float[PMD_IMAGE_SIZE]; // 19800 * 4 bytes
	}

	// static variables
	private static Map<AirTouchPoint.TouchType, Paint> paintBrushes = new HashMap<AirTouchPoint.TouchType, Paint>();
	static Paint defaultPaintBrush;
	static Paint textPaintBrush;

	// Networking
	DataOutputStream _toServer;
	InputStream _fromServer;
	PMDSendData _dataFromServer;
	boolean _stopNetworkConnection = false;
	boolean _getOnlyFingerData = false;
	Timer _timer;

	// UI
	private Map<Integer, AirTouchPoint> _touchMap = new HashMap<Integer, AirTouchPoint>();
	Object _touchMapLock = new Object();
	String _errorText;
	RectF _touchDrawRect = new RectF();
	Bitmap _pmdDepth = Bitmap.createBitmap(PMD_NUM_COLS, PMD_NUM_ROWS, Bitmap.Config.ARGB_8888);
	Matrix _depthMatrix = new Matrix();

	// bitmap to render the depth data
	private Map<Integer, LinkedList<PMDFinger>> _recentPoints = new HashMap<Integer, LinkedList<PMDFinger>>();
	private int _recentPointsCount = 30;
	Object _recentPointsLock = new Object();
	static
	{
		defaultPaintBrush = new Paint();
		defaultPaintBrush.setColor(Color.BLACK);
		defaultPaintBrush.setTextSize(20);

		textPaintBrush = new Paint();
		textPaintBrush.setColor(Color.RED);
		textPaintBrush.setTextSize(40);

		Paint paint;

		paint = new Paint();
		paint.setColor(Color.MAGENTA);
		paintBrushes.put(AirTouchPoint.TouchType.TOUCH_DOWN, paint);

		paint = new Paint();
		paint.setColor(Color.GREEN);
		paintBrushes.put(AirTouchPoint.TouchType.TOUCH_MOVE, paint);

		paint = new Paint();
		paint.setColor(Color.BLUE);
		paint.setStrokeCap(Cap.ROUND);
		paintBrushes.put(AirTouchPoint.TouchType.AIR_MOVE1, paint);
		
		paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStrokeCap(Cap.ROUND);
		paintBrushes.put(AirTouchPoint.TouchType.AIR_MOVE2, paint);
		
	}

	public AirTouchView(Context context) {
		super(context);
	}

	public AirTouchView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirTouchView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void shouldIGetOnlyFingerData(boolean shouldIGetFingerData)
	{
		_getOnlyFingerData = shouldIGetFingerData;
	}
	public void setupServerConnection(InputStream fromServer, DataOutputStream toServer)
	{
		_fromServer = fromServer;
		_toServer = toServer;
	}

	public Point3f PhoneToScreen(PMDFinger f)
	{
		Point3f result = new Point3f();
		float realDepth = 0.1f;
		WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		
		int width = display.getWidth();
		int height = display.getHeight();
		
		result.x = f.x  * width;
		result.y = f.z  * height;
		result.z = f.y / realDepth;
		return result;
	}
	
	protected void onDraw(Canvas canvas) {
		canvas.drawRGB(255, 255, 255);
		Paint paint;

		
		// Draw touch points
		synchronized(_touchMapLock) {

			for(AirTouchPoint tp : _touchMap.values()) {
				canvas.save();

				canvas.translate(tp.x, tp.y);
				paint = paintBrushes.get(tp.type);
				if(paint == null) paint = defaultPaintBrush;
				_touchDrawRect.set(
						-TOUCH_SCALE, -TOUCH_SCALE,
						TOUCH_SCALE, TOUCH_SCALE);
				canvas.drawOval(_touchDrawRect, paint);

				canvas.restore();
			}
		}


		// draw a bitmap with the bytes if we are not just showing finger data.
		if(!_getOnlyFingerData) canvas.drawBitmap(_pmdDepth, _depthMatrix, null);
		

		if(_touchMap.values().size() == 0)
		{
			for (int i = 0; i < _dataFromServer.fingers.length; i++) {
				if(_dataFromServer.fingers[i] == null) continue;

				if(_dataFromServer.fingers[i].id >= 0)
				{
					
					Point3f p = PhoneToScreen(_dataFromServer.fingers[i]);
					
					TouchType type = _dataFromServer.fingers[i].id % 2 == 0 ? TouchType.AIR_MOVE1 : TouchType.AIR_MOVE2;
					paintBrushes.get(type).setAlpha((int)(1 - 255 * Math.max(0, Math.min(1, p.z) ) ));
					
					canvas.drawCircle(p.x, p.y, p.z * 500, paintBrushes.get(type));	
					paintBrushes.get(type).setAlpha(1);
				}
			} 
		} else
		{
			
			synchronized(_recentPointsLock){
				
				for (Entry<Integer, LinkedList<PMDFinger>> paths : _recentPoints.entrySet()) {
					
					int i = 0;
					PMDFinger prev = new PMDFinger();
					for (PMDFinger p : paths.getValue()) {
						System.out.println("drawing points " + p.x + ", " + p.y + "," + p.z);
						TouchType type = p.id % 2 == 0 ? TouchType.AIR_MOVE1 : TouchType.AIR_MOVE2;
						
						paintBrushes.get(type).setAlpha((int)(255 * ( (float)i / paths.getValue().size() )));
						if(i != 0){
							paintBrushes.get(type).setStrokeWidth(p.z * 100);
							canvas.drawLine(prev.x, prev.y, p.x, p.y, paintBrushes.get(type));
						}
						prev.x = p.x;
						prev.y = p.y;
						//canvas.drawCircle(p.x, p.y, p.z * 100, paintBrushes.get(type));
						i++;
					}
				}	
			}
			
		}


		// Draw any errors
		if(_errorText != null) 
		{
			canvas.drawText(_errorText, 20, 50, textPaintBrush);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		String touchStr = "";
		AirTouchPoint.TouchType type;
		int action = event.getActionMasked();

		switch(action)
		{
		case MotionEvent.ACTION_DOWN:
			touchStr = "TOUCH_DOWN";
			type = AirTouchPoint.TouchType.TOUCH_DOWN;
			break;
		case MotionEvent.ACTION_UP:
			performClick();
			touchStr = "TOUCH_UP";
			type = AirTouchPoint.TouchType.TOUCH_UP;
			break;
		case MotionEvent.ACTION_MOVE:
			touchStr = "TOUCH_MOVE";
			type = AirTouchPoint.TouchType.TOUCH_MOVE;
			break;
		default:
			return false;
		}


		for (int i = 0; i < event.getPointerCount(); i++) 
		{
			int id = event.getPointerId(i);

			float x = event.getX(i);
			float y = event.getY(i);

			// send touch data to server
			//			String sendStr = id + ":" + touchStr + ":" + x + "," + y; 
			// new SendStringTask().execute(sendStr + "\n");
			//			Log.v(AirTouchViewMain.TAG, sendStr);
			synchronized(_touchMapLock)
			{
				if(action != MotionEvent.ACTION_UP)
				{
					_touchMap.put(id, new AirTouchPoint(x, y, type));	
				} 
				else 
				{
					if(_touchMap.containsKey(id)){
						_touchMap.remove(id);
					}
					if(_touchMap.size() == 0)
					{
						synchronized(_recentPointsLock){
							_recentPoints.clear();	
						}
						
					}
				}				
			}

		}
		invalidate();
		return true;

	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		_errorText = null;

		_depthMatrix.setScale(2.0f, -2.0f);
		_depthMatrix.postTranslate(0, 240);
		//_depthMatrix.setScale(2.0f, 2.0f);
		_dataFromServer = new PMDSendData();
		// handshake has already happened
		// begin sending and receiving data
		TimerTask task = new TimerTask() {
            public void run() {
                new SendReceiveTask().execute();
                
            }
        };
        _timer = new Timer();
        _timer.scheduleAtFixedRate(task, 1, 33);
	}

	public void stop()
	{
		_stopNetworkConnection = true;
		_timer.cancel();
		new DisconnectTask().execute();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		// garbage collect the pmd data
		_dataFromServer = null;
		_stopNetworkConnection = true;
		// stop sending and receiving data
		// send disconnect message?
	}

	public void updateRecentPoints()
	{
		if(_touchMap.values().size() > 0) return;
		synchronized(_recentPointsLock)
		{
			for (int i = 0; i < _dataFromServer.fingers.length; i++) {
				if(_dataFromServer.fingers[i].id < 0) continue;
				if(!_recentPoints.containsKey(_dataFromServer.fingers[i].id))
				{
					_recentPoints.put(_dataFromServer.fingers[i].id, new LinkedList<PMDFinger>());
				}
				LinkedList<PMDFinger> lst = _recentPoints.get(_dataFromServer.fingers[i].id);
				while(lst.size() > _recentPointsCount)
				{
					lst.pop();
				}
				Point3f p = PhoneToScreen(_dataFromServer.fingers[i]);
				PMDFinger toAdd = new PMDFinger();
				toAdd.id = _dataFromServer.fingers[i].id;
				toAdd.x = p.x;
				toAdd.y = p.y;
				toAdd.z = p.z;
				lst.add(toAdd);			
			}			
		}


	}
	
	//
	// PMDData methods
	//

	/**
	 * Constructs an object from an array of bytes that is 
	 * assumed to have the struct as defined in pmddata.h 
	 * (see https://github.com/devlabcmu/projects/wiki/PMD-Constants)
	 * @param in
	 * @return The resulting struct
	 */
	public void updatePMDData(byte[] in)
	{
		if(_dataFromServer == null) return;

		for (int i = 0; i < _dataFromServer.fingers.length; i++) 
		{
			_dataFromServer.fingers[i] = new PMDFinger();
			_dataFromServer.fingers[i].id = getIntInByteArray(in, i * 16);
			_dataFromServer.fingers[i].x = getFloatInByteArray(in, i * 16 + 4);
			_dataFromServer.fingers[i].y = getFloatInByteArray(in, i * 16 + 8);
			_dataFromServer.fingers[i].z = getFloatInByteArray(in, i * 16 + 12);
		}
		
		if(!_getOnlyFingerData)
		{
			for (int i = 0; i < PMD_IMAGE_SIZE; i++) {
				_dataFromServer.buffer[i] = getFloatInByteArray(in, 24 + i * 4);
			}	
		}
		

		if(OUTPUT_BITS_DEBUG){
			// if we want we can output the raw bits of the finger x, y, z positions.
			for (int i = 0; i < 3; i++) {
				StringBuilder sb = new StringBuilder();

				for (int j = 0; j < 4; j++) {
					byte b = in[i * 4 + j];
					for (int k = 0; k < 8; k++) {
						int n = (b & (1 << k)) >> k;
						sb.append("" + n);
					}
					sb.append( " ");
				}
				Log.v(TAG, String.format("i: %d, bits: %s\n", i, sb.reverse().toString()));
			}
		}
		//Log.v(TAG, String.format("%.2f, %.2f, %.2f", _dataFromServer.fingerX, _dataFromServer.fingerY, _dataFromServer.fingerZ));
	}
	
	/**
	 * Gets a float at a specified offset from a byte array.
	 * This should be a general utility
	 * @param bytes
	 * @param startOffset The start of the array to look at, in bytes. Shoudl increment in steps of 4 for floats
	 * @return
	 */
	public static float getFloatInByteArray(byte[] bytes, int startOffset)
	{
		// 0xF is 4 bits, 0xFF is 8 bits
		int asInt = (bytes[startOffset + 0] & 0xFF) 
				| ((bytes[startOffset + 1] & 0xFF) << 8) 
				| ((bytes[startOffset + 2] & 0xFF) << 16) 
				| ((bytes[startOffset + 3] & 0xFF) << 24);
		return Float.intBitsToFloat(asInt);
	}
	
	public static int getIntInByteArray(byte[] bytes, int startOffset)
	{
		return (bytes[startOffset + 0] & 0xFF) 
				| ((bytes[startOffset + 1] & 0xFF) << 8) 
				| ((bytes[startOffset + 2] & 0xFF) << 16) 
				| ((bytes[startOffset + 3] & 0xFF) << 24);
	}

	class DisconnectTask extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params) {
			_timer.cancel();
			try {
				_toServer.writeBytes("disconnect");
			} catch (IOException e) {
				Log.v(TAG, "Exception when disconnecting: " + e.getMessage());
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			_errorText  = "Error: disconnected from server due to an error";
			postInvalidate();
		}
	}

	class SendReceiveTask extends AsyncTask<Void, Void, Boolean>
	{
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		@Override
		protected Boolean doInBackground(Void... params) {
		   // Log.v(TAG, "in SendReceiveData");
			try {
				// send 'gimme'
				// receive data
				// update PMDSendData
				if(_getOnlyFingerData)
				{
					//Log.v(TAG, "wrote finger");
					_toServer.writeBytes("finger");
				} else 
				{
					_toServer.writeBytes("gimme");
				}
				byte[] lMsg = new byte[PMD_SEND_DATA_SIZE];
				int nleft = PMD_SEND_DATA_SIZE;
				if(_getOnlyFingerData) nleft = PMD_FINGER_ONLY_DATA_SIZE;
				int totalReceived = 0;

				do{
					if (nleft == 0) break;
					int nReceived = 0;

					nReceived = _fromServer.read(lMsg, totalReceived, nleft);				
					if(nReceived <= 0) break;
					totalReceived += nReceived;
					nleft -= nReceived;
					///Log.v(TAG, String.format("recevied %d bytes, total received %d, %d left", nReceived, totalReceived, nleft));
				} while (true);
				//Log.v(TAG, "got data");
				updatePMDData(lMsg);
				updateRecentPoints();
				//Log.v(TAG, "updated data");
			} catch (IOException e) {
				Log.v(TAG, e.getMessage());
				return false;
			}
			return true;
		}
		@Override
		protected void onPostExecute(Boolean succeeded) {
			//Log.v(TAG, "in post execute");
			// if we failed, then we should disconnect and go back to the home page
			if(!succeeded) {
				_errorText = "ERROR: couldn't communicate with server. Please go back and try again.";
				postInvalidate();
				new DisconnectTask().execute();
				return;
			}
			
			if(_stopNetworkConnection) return;

			postInvalidate();
		}
	}

}
