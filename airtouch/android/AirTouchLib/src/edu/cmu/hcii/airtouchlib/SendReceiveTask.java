package edu.cmu.hcii.airtouchlib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.os.AsyncTask;
import android.util.Log;



public class SendReceiveTask extends AsyncTask<Void, Void, Boolean>
{
	
	static final String TAG ="SendReceiveTask";
	public class PMDSendData {
		public PMDFinger[] fingers = new PMDFinger[2]; 
		public float[] buffer = new float[PMDConstants.PMD_IMAGE_SIZE]; // 19800 * 4 bytes
		public long timestamp;
	}
	

	private final PMDServerConnection _server;
	private final List<PMDDataHandler> _handlers = new ArrayList<PMDDataHandler>();
	private boolean _getOnlyFingerData;
	static final boolean OUTPUT_BITS_DEBUG = false;
	private PMDSendData _dataFromServer = new PMDSendData();
	byte[] _lMsg = new byte[PMDConstants.PMD_SEND_DATA_SIZE];
	
	public SendReceiveTask(PMDServerConnection server, boolean fingersOnly, PMDDataHandler ... handlers) {
		this._server = server;
		for(int i = 0; i < handlers.length; i++)
			addHandler(handlers[i]);
		_getOnlyFingerData = fingersOnly;
		_dataFromServer = new PMDSendData();
	}
	
	public void addHandler(PMDDataHandler handler)
	{
		_handlers.add(handler);
	}
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}
	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			if(_getOnlyFingerData)
			{
				_server._outToServer.writeBytes("finger");
			} else 
			{
				_server._outToServer.writeBytes("gimme");
			}
			
			int nleft = PMDConstants.PMD_SEND_DATA_SIZE;
			if(_getOnlyFingerData) nleft = PMDConstants.PMD_FINGER_ONLY_DATA_SIZE;
			int totalReceived = 0;

			do{
				if (nleft == 0) break;
				int nReceived = 0;

				nReceived = _server._inFromServer.read(_lMsg, totalReceived, nleft);				
				if(nReceived <= 0) break;
				totalReceived += nReceived;
				nleft -= nReceived;
			} while (true);
			updatePMDData(_lMsg);
		} catch (IOException e) {
			Log.i(TAG, e.getMessage());
			return false;
		}
		return true;
	}
	@Override
	protected void onPostExecute(Boolean succeeded) {
		//Log.i(TAG, "in post execute");
		// if we failed, then we should disconnect and go back to the home page
		if(!succeeded) {
			// todo: update UI somehow with an error
			for (PMDDataHandler handler : _handlers) 
				handler.onSendReceiveTaskFailed("ERROR: couldn't communicate with server. Please go back and try again.");	
			
			// this.airTouchView._errorText = "ERROR: couldn't communicate with server. Please go back and try again.";
			// this.airTouchView.postInvalidate();
			new DisconnectTask(_server).execute();
			return;
		}
		for (PMDDataHandler handler : _handlers) 
			handler.newPMDData(_dataFromServer);
		
		_dataFromServer = null;
		_lMsg = null;
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
		
		long now = System.currentTimeMillis();
		for (int i = 0; i < _dataFromServer.fingers.length; i++) 
		{
			_dataFromServer.fingers[i] = new PMDFinger();
			_dataFromServer.fingers[i].id = getIntInByteArray(in, i * 16);
			_dataFromServer.fingers[i].x = getFloatInByteArray(in, i * 16 + 4);
			_dataFromServer.fingers[i].y = getFloatInByteArray(in, i * 16 + 8);
			_dataFromServer.fingers[i].z = getFloatInByteArray(in, i * 16 + 12);
			_dataFromServer.fingers[i].timestamp = now;
		}
		
		if(!_getOnlyFingerData)
		{
			for (int i = 0; i < PMDConstants.PMD_IMAGE_SIZE; i++) {
				_dataFromServer.buffer[i] = getFloatInByteArray(in, 24 + i * 4);
			}	
		}
		
		_dataFromServer.timestamp = now;
		
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
				Log.i(TAG, String.format("i: %d, bits: %s\n", i, sb.reverse().toString()));
			}
		}
//		Log.i(TAG, String.format("%.2f, %.2f, %.2f", _dataFromServer.fingers[0].x, _dataFromServer.fingers[0].y, _dataFromServer.fingers[0].z));
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
}