package edu.cmu.hcii.airtouchview;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
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
	static final int PMD_FINGER_ONLY_DATA_SIZE = 24;
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
	
	// structs from PMD
	public class PMDSendData {
		public float finger1X; // 4 bytes
		public float finger1Y; // 4 bytes
		public float finger1Z; // 4 bytes
		
		public float finger2X; // 4 bytes
		public float finger2Y; // 4 bytes
		public float finger2Z; // 4 bytes
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
		paintBrushes.put(AirTouchPoint.TouchType.AIR_MOVE1, paint);
		
		paint = new Paint();
		paint.setColor(Color.RED);
		paintBrushes.put(AirTouchPoint.TouchType.AIR_MOVE2, paint);
		
	}

	public AirTouchView(Context context) {
		super(context);
		// TODO Auto-generated constructor stubrere
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

	public Point3f PhoneToScreen(float x, float y, float z)
	{
		Point3f result = new Point3f();
		float realWidth = 0.065f;
		float realHeight = 0.095f;
		float realDepth = 0.1f;
		WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		
		int width = display.getWidth();
		int height = display.getHeight();
		
		result.x =  x / realWidth * width;
		result.y = z / realHeight * height;
		result.z = y / realDepth;
		return result;
	}
	
	protected void onDraw(Canvas canvas) {
		Log.v("AirTouchView", "drawing");
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
		
		// convert the finger 
		
		// draw a circle at Finger loc
		if(_dataFromServer.finger1X > PMD_INVALID_DISTANCE )
		{
			Point3f p = PhoneToScreen(_dataFromServer.finger1X, _dataFromServer.finger1Y, _dataFromServer.finger1Z);
//			Log.v("AirTouchView", String.format("finger1 pos: (%f, %f, %f)\n\t(%f, %f, %f)", 
//					_dataFromServer.finger1X, _dataFromServer.finger1Y, _dataFromServer.finger1Z,
//					p.x, p.y, p.z
//					));
			
			canvas.drawCircle(p.x, p.y, p.z * 500, paintBrushes.get(TouchType.AIR_MOVE1));
		}
		
		if (_dataFromServer.finger2X > PMD_INVALID_DISTANCE)
		{
			Point3f p = PhoneToScreen(_dataFromServer.finger2X, _dataFromServer.finger2Y, _dataFromServer.finger2Z);
//			Log.v("AirTouchView", String.format("finger2 pos: (%f, %f, %f)\n\t(%f, %f, %f)", 
//					_dataFromServer.finger2X, _dataFromServer.finger2Y, _dataFromServer.finger2Z,
//					p.x, p.y, p.z
//					));
			canvas.drawCircle(p.x, p.y, p.z * 500, paintBrushes.get(TouchType.AIR_MOVE2));			
		}

		// Draw any errors
		if(_errorText != null) 
		{
			canvas.drawText(_errorText, 20, 50, textPaintBrush);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
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

		_dataFromServer.finger1X = getFloatInByteArray(in, 0);
		_dataFromServer.finger1Y = getFloatInByteArray(in, 4);
		_dataFromServer.finger1Z = getFloatInByteArray(in, 8);

		_dataFromServer.finger2X = getFloatInByteArray(in, 12);
		_dataFromServer.finger2Y = getFloatInByteArray(in, 16);
		_dataFromServer.finger2Z = getFloatInByteArray(in, 20);
		
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
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			_errorText  = "Error: disconnected from server due to an error";
			postInvalidate();
		}
	}

	class SendReceiveTask extends AsyncTask<Void, Void, Boolean>
	{
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
		}
		@Override
		protected Boolean doInBackground(Void... params) {
			// TODO Auto-generated method stub
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
				//Log.v(TAG, "updated data");
			} catch (IOException e) {
				Log.v(TAG, e.getMessage());
				return false;
			}
			return true;
		}
		@Override
		protected void onPostExecute(Boolean succeeded) {
			// TODO Auto-generated method stub
			//Log.v(TAG, "in post execute");
			// if we failed, then we should disconnect and go back to the home page
			if(!succeeded) {
				_errorText = "ERROR: couldn't communicate with server. Please go back and try again.";
				postInvalidate();
				new DisconnectTask().execute();
				return;
			}
			
			if(_stopNetworkConnection) return;

			// invalidate screen
			// update pixels
//			int i = 0;
//			for (int y = 0; y < PMD_NUM_ROWS; y++) {
//				for (int x = 0; x < PMD_NUM_COLS; x++, i++) {
//					float val = _dataFromServer.buffer[i];
//					int v = 0;
//					if(val <= 1.0f)
//					{
//						v = (int)(255 - 255 * val);
//					}
//					_pmdDepth.setPixel(x, y, Color.argb(255, v,v,v));
//				}
//			}
			// Log.v(TAG, "invalidating");
			postInvalidate();
		}
	}

}
