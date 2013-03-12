package edu.cmu.hcii.airtouchpaint;

import android.graphics.Path;
import android.view.MotionEvent;

public class StrokeCrop extends PathCrop {

	public StrokeCrop(AirTouchPaintView parent) {
		super(parent);
	}

	
	public StrokeCrop(AirTouchPaintView parent, Stroke s)
	{
		this(parent);
		Path p = new Path(s.getPath());
		p.close();
		m_clipPath = p;
	}
}
