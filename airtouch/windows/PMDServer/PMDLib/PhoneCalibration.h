#pragma once
#include <opencv/cxcore.h>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/calib3d/calib3d.hpp>

using namespace cv;
class PhoneCalibration
{
public:
	PhoneCalibration(void);
	~PhoneCalibration(void);
	Point3f ToPhoneSpace(Point3f coord);
	void ToPhoneSpace(float* src, float* dst);
	Point3f ToPhoneSpaceAsPercentage(Point3f coord);
private:
	Mat m_origin;
	Mat m_unitX;
	Mat m_unitY;
	Mat m_unitZ;
	float m_xLength;
	float m_yLength;
	float m_zLength;
};

class RBTPhoneCalibration
{

public:
	RBTPhoneCalibration();
	~RBTPhoneCalibration(){};

	Point3f ToPhoneSpace(Point3f coord);
	// returns coords in phone space, x,y are the percentage of the screen width, z is still in meters
	
private:
	Mat m_rotationMatrix;
	Mat m_translation;



};

