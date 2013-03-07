package edu.cmu.hcii.airtouchlib;

public class PMDFinger {
	public int id;
	public float x;
	public float y;
	public float z;
	public long timestamp;
	public PMDFinger(){}
	public PMDFinger(PMDFinger copyMe)
	{
		id = copyMe.id;
		x = copyMe.x;
		y = copyMe.y;
		z = copyMe.z;
		timestamp = copyMe.timestamp;
	}
}
