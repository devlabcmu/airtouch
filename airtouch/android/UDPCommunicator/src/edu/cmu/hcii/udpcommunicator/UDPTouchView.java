package edu.cmu.hcii.udpcommunicator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.content.Context;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class UDPTouchView extends View {

	DatagramSocket _socket;
	int _port;
	InetAddress _serverAddr;
	
	public UDPTouchView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public UDPTouchView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public UDPTouchView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		String touchStr = "";
		switch(event.getActionMasked())
		{
		case MotionEvent.ACTION_DOWN:
			touchStr = "TOUCH_DOWN";
			break;
		case MotionEvent.ACTION_UP:
			performClick();
			touchStr = "TOUCH_UP";
			break;
		case MotionEvent.ACTION_MOVE:
			touchStr = "TOUCH_MOVE";
			break;
		default:
			return false;
		}
		touchStr += " : " + event.getRawX() + " , " + event.getRawY() + " ; ";
		new SendStringTask().execute(touchStr);
		Log.v(UDPCommunicatorMain.TAG, touchStr);
		
		return true;
		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
//		canvas.drawRGB(255, 255, 255);
	}
	
	public void initializeConnection(DatagramSocket socket, int port, InetAddress serverAddr)
	{
		_socket = socket;
		_port = port;
		_serverAddr = serverAddr;
	}

	// TODO: refactor out to use member variables...
	class SendStringTask extends AsyncTask<String, Void, Boolean>
    {
		@Override
		protected Boolean doInBackground(String... params) {
	    	if(_socket == null)
	    	{
	    		Log.v(UDPCommunicatorMain.TAG, "Error: Tried to send string but socket was null");
	    		return false;

	    	}
	    	for (String s : params) 
	    	{
				DatagramPacket dp;
				dp = new DatagramPacket(s.getBytes(), s.length(), _serverAddr, _port);
				try {
					_socket.send(dp);
				} catch (IOException e) {
					Log.v(UDPCommunicatorMain.TAG, "error sending string: " + e.getMessage());
					return false;
				}				
			}

			return true;
		}
    }
}
