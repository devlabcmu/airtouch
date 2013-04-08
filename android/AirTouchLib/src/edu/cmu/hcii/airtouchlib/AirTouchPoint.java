package edu.cmu.hcii.airtouchlib;



public class AirTouchPoint {
	public float x;
	public float y;
	public TouchType type;
	
	public enum TouchType
	{
		TOUCH_DOWN,
		TOUCH_MOVE,
		TOUCH_UP,
		AIR_MOVE1,
		AIR_MOVE2,
		AIR_REMOVED
	}
	public AirTouchPoint(float x, float y, TouchType t){
		this.x = x;
		this.y = y;
		this.type = t;
	}
}


