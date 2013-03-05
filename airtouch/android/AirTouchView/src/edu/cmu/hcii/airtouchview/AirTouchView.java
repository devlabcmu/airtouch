package edu.cmu.hcii.airtouchview;

import java.sql.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import edu.cmu.hcii.airtouchlib.AirTouchPoint;
import edu.cmu.hcii.airtouchlib.AirTouchPoint.TouchType;
import edu.cmu.hcii.airtouchlib.DisconnectTask;
import edu.cmu.hcii.airtouchlib.PMDConstants;
import edu.cmu.hcii.airtouchlib.PMDDataHandler;
import edu.cmu.hcii.airtouchlib.PMDFinger;
import edu.cmu.hcii.airtouchlib.PMDServerConnection;
import edu.cmu.hcii.airtouchlib.SendReceiveTask;
import edu.cmu.hcii.airtouchlib.SendReceiveTask.PMDSendData;


public class AirTouchView extends View implements PMDDataHandler {

	public class Point3f
	{
		public float x;
		public float y;
		public float z;
		
	}
	
	// Constants
	static final int TOUCH_SCALE = 50;
	

	static String TAG = "AirTouchViewView";

	// static variables
	private static Map<AirTouchPoint.TouchType, Paint> paintBrushes = new HashMap<AirTouchPoint.TouchType, Paint>();
	static Paint defaultPaintBrush;
	static Paint textPaintBrush;

	// Networking
	PMDServerConnection _connection;
	PMDSendData _dataFromServer;
	boolean _stopNetworkConnection = false;
	boolean _getOnlyFingerData = false;
	Timer _timer;

	// UI
	private Map<Integer, AirTouchPoint> _touchMap = new HashMap<Integer, AirTouchPoint>();
	Object _touchMapLock = new Object();
	String _errorText;
	RectF _touchDrawRect = new RectF();
	Bitmap _pmdDepth = Bitmap.createBitmap(PMDConstants.PMD_NUM_COLS, PMDConstants.PMD_NUM_ROWS, Bitmap.Config.ARGB_8888);
	Matrix _depthMatrix = new Matrix();

	// bitmap to render the depth data
	@SuppressLint("UseSparseArrays")
	private Map<Integer, LinkedList<PMDFinger>> _recentPoints = new HashMap<Integer, LinkedList<PMDFinger>>();
	private int _recentPointsCount = 30;
	Object _recentPointsLock = new Object();
	static
	{
		defaultPaintBrush = new Paint();
		defaultPaintBrush.setColor(Color.BLACK);
		defaultPaintBrush.setTextSize(40);

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
	public void setServerConnection(PMDServerConnection c)
	{
		_connection = c;
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
		

		if(_touchMap.values().size() == 0 && _dataFromServer !=null)
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
		canvas.drawText(String.format("pmd data fps: %.2f", _pmdFPS), 20, 100, defaultPaintBrush);
		
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
		final PMDDataHandler me = this;
		//_depthMatrix.setScale(2.0f, 2.0f);
		// handshake has already happened
		// begin sending and receiving data
		TimerTask task = new TimerTask() {
            public void run() {
                new SendReceiveTask(_connection, me, _getOnlyFingerData).execute();
                
            }
        };
        _timer = new Timer();
        _timer.scheduleAtFixedRate(task, 1, 20);
	}

	public void stop()
	{
		_stopNetworkConnection = true;
		_timer.cancel();
		new DisconnectTask(_connection).execute();
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
	
	long _lastPmdDataTime = 0;
	float _pmdFPS = 0;
	int _fpsCounter = 0;
	long _pmdInterval = 0;

	public void updateFPS()
	{
		long now = System.currentTimeMillis();
		if(_lastPmdDataTime != 0){
			_pmdInterval = (now - _lastPmdDataTime);
			if(_pmdInterval > 1000)
			{
				_pmdFPS = _fpsCounter;
				_fpsCounter = 0;
				_lastPmdDataTime = now;
			}
		} else
		{
			_lastPmdDataTime = now;	
		}
		_fpsCounter++;
	}
	
	@Override
	public void NewPMDData(PMDSendData data) {
		_dataFromServer = data;
		updateRecentPoints();
		updateFPS();
		postInvalidate();
	}

	@Override
	public void OnSendReceiveTaskFailed(String message) {
		// TODO Auto-generated method stub
		
	}
	


}
