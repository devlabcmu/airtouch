package edu.cmu.hcii.airtouchview;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


public class AirTouchView extends View {
	static final int TOUCH_SCALE = 50;
	
	DatagramSocket _socket;
	int _port;
	InetAddress _serverAddr;
	
	private Map<Integer, AirTouchPoint> _touchMap = new HashMap<Integer, AirTouchPoint>();
	private static Map<AirTouchPoint.TouchType, Paint> paintBrushes = new HashMap<AirTouchPoint.TouchType, Paint>();
	static Paint defaultPaintBrush;
	Object _touchMapLock = new Object();
	
	RectF _touchDrawRect = new RectF();
	
	static
	{
		defaultPaintBrush = new Paint();
		defaultPaintBrush.setColor(Color.GRAY);
		Paint paint;
		
		paint = new Paint();
		paint.setColor(Color.RED);
		paintBrushes.put(AirTouchPoint.TouchType.TOUCH_DOWN, paint);

		paint = new Paint();
		paint.setColor(Color.GREEN);
		paintBrushes.put(AirTouchPoint.TouchType.TOUCH_MOVE, paint);

		paint = new Paint();
		paint.setColor(Color.BLUE);
		paintBrushes.put(AirTouchPoint.TouchType.AIR_MOVE, paint);
	}
	
	public AirTouchView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public AirTouchView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AirTouchView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	
	
	protected void onDraw(Canvas canvas) {
		canvas.drawRGB(255, 255, 255);
		Paint paint;

		synchronized(_touchMapLock) {
			
			for(AirTouchPoint tp : _touchMap.values()) {
				Log.v(AirTouchViewMain.TAG, "draw point " + tp.x + "," + tp.y);
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
			String sendStr = id + ":" + touchStr + ":" + x + "," + y; 
			new SendStringTask().execute(sendStr + "\n");
			Log.v(AirTouchViewMain.TAG, sendStr);
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
	
	public void initializeConnection(DatagramSocket socket, int port, InetAddress serverAddr)
	{
		_socket = socket;
		_port = port;
		_serverAddr = serverAddr;
		
		new ReceiveAirEventTask().execute();
	}

	class ReceiveAirEventTask extends AsyncTask<Void, Void, String>
    {
    	@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		Log.v(AirTouchViewMain.TAG, "receiving on port " + _port + "...");
    	}
		@Override
		protected String doInBackground(Void... params) {
			// TODO Auto-generated method stub
			if(_socket == null) return null;
			if(_socket.isClosed()) return null;
			String result;
			byte[] lMsg = new byte[AirTouchViewMain.MAX_UDP_DATAGRAM_LEN];
			DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length);
			try {
				_socket.setSoTimeout(500);
				_socket.receive(dp);
			}catch (InterruptedIOException e)
			{
				return null;
			}catch (IOException e) {
				return null;
			} 
			result = new String(lMsg, 0, dp.getLength());
			Log.v(AirTouchViewMain.TAG, "UDP packet received " + result);
			return result;
		}
		@Override
		protected void onPostExecute(String result) {
			// update editText
			if(result != null)
			{

				String[] idsplit = result.split(":");
				if(idsplit.length != 2){
					Log.v(AirTouchViewMain.TAG, "Text not in valid form: " + result);
				} else
				{
					int id = Integer.parseInt(idsplit[0]);
					String[] pos = idsplit[1].split(",");
					if(pos.length != 3){
						Log.v(AirTouchViewMain.TAG, "Invalid position string: " + idsplit[1]);

					} else {
						float x = Float.parseFloat(pos[0]);
						float y = Float.parseFloat(pos[1]);
						float z = Float.parseFloat(pos[2]);
						synchronized (_touchMapLock) {
							_touchMap.put(id, new AirTouchPoint(x, y, AirTouchPoint.TouchType.AIR_MOVE));
						}
						invalidate();
					}
				}
			}
			if(_socket != null)
			{
				new ReceiveAirEventTask().execute();
			}
		}
    }
	
	// TODO: refactor out to use member variables...
	class SendStringTask extends AsyncTask<String, Void, Boolean>
    {
		@Override
		protected Boolean doInBackground(String... params) {
	    	if(_socket == null)
	    	{
	    		Log.v(AirTouchViewMain.TAG, "Error: Tried to send string but socket was null");
	    		return false;

	    	}
	    	for (String s : params) 
	    	{
				DatagramPacket dp;
				dp = new DatagramPacket(s.getBytes(), s.length(), _serverAddr, _port);
				try {
					_socket.send(dp);
				} catch (IOException e) {
					Log.v(AirTouchViewMain.TAG, "error sending string: " + e.getMessage());
					return false;
				}				
			}

			return true;
		}
    }

}
