package edu.cmu.hcii.airtouchview;



public class AirTouchPoint {
	float x;
	float y;
	TouchType type;
	
	public enum TouchType
	{
		TOUCH_DOWN,
		TOUCH_MOVE,
		TOUCH_UP,
		AIR_MOVE,
		AIR_REMOVED
	}
	public AirTouchPoint(float x, float y, TouchType t){
		this.x = x;
		this.y = y;
		this.type = t;
	}
}


