#pragma once
#include <opencv/cxcore.h>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/calib3d/calib3d.hpp>

using namespace cv;

enum PhoneCalibrationCoord
{
	UPPER_LEFT = 0,
	UPPER_RIGHT,
	LOWER_LEFT,
	PHONE_CALIBRATION_COORD_COUNT
};

class PhoneCalibration
{
public:
	PhoneCalibration(void);
	~PhoneCalibration(void);

	
	Point3f GetPhoneUpperLeft(){return m_origin;}
	Point3f GetPhoneUpperRight(){return m_upperRight;}
	Point3f GetPhoneLowerLeft(){return m_lowerLeft;}
	
	void SetPhoneUpperLeft(Point3f upperLeft){m_origin = upperLeft; UpdateBasis(); WriteToFile();}
	void SetPhoneUpperRight(Point3f upperRight){m_upperRight = upperRight; UpdateBasis();WriteToFile();}
	void SetPhoneLowerLeft(Point3f lowerLeft){m_lowerLeft = lowerLeft; UpdateBasis();WriteToFile();}

	void SetPhoneCalibrationCoord(PhoneCalibrationCoord type, Point3f world)
	{
		switch(type){
		case UPPER_LEFT:
			SetPhoneUpperLeft(world);
			break;
		case UPPER_RIGHT:
			SetPhoneUpperRight(world);
			break;
		case LOWER_LEFT:
			SetPhoneLowerLeft(world);
			break;
		}
	}

	void WriteToFile();
	void InitFromFile();

	Point3f ToPhoneSpace(Point3f coord);
	void ToPhoneSpace(float* src, float* dst);
	Point3f ToPhoneSpaceAsPercentage(Point3f coord);
private:
	Point3f m_origin;
	Vec3f m_unitX;
	Vec3f m_unitY;
	Vec3f m_unitZ;
	
	Point3f m_upperRight;
	Point3f m_lowerLeft;

	float m_xLength;
	float m_yLength;
	float m_zLength;

	void UpdateBasis();

};


