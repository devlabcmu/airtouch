#include "PhoneCalibration.h"
#include "PMDConstants.h"
#include <iostream>
#include <fstream>
using namespace std;
#define PHONE_ANDROID

#ifdef PHONE_ANDROID

#define ORIGINX 0.0318f
#define ORIGINY -0.0355f
#define ORIGINZ 0.0737f

#define UPPERRIGHTX -0.0201f
#define UPPERRIGHTY -0.0347f
#define UPPERRIGHTZ 0.0672f

#define LOWERLEFTX 0.0269f
#define LOWERLEFTY -0.0310f
#define LOWERLEFTZ 0.1718f
#define CALIB_FILE "nexuss_calibration.csv"
#endif


#ifdef PHONE_ANDROID_1_31
#define ORIGINX 0.0324f
#define ORIGINY -0.0287f
#define ORIGINZ 0.0552

#define P1X -0.0174f
#define P1Y -0.0275f
#define P1Z 0.0545f

#define P2X 0.0437f
#define P2Y -0.0209f
#define P2Z  0.1860f
#define CALIB_FILE "nexuss_calibration.csv"
#endif

#ifdef PHONE_IPHONE
// calibration for iphone
#define ORIGINX 0.0137f
#define ORIGINY -0.0098f
#define ORIGINZ 0.0147

#define P1X -0.0174f
#define P1Y -0.0221f
#define P1Z  0.0401f

#define P2X  0.0352f
#define P2Y -0.0067f
#define P2Z  0.1238f
#define CALIB_FILE "iphone_calibration.csv"
#endif


PhoneCalibration::PhoneCalibration(void)
{
	m_origin = Point3f( ORIGINX , ORIGINY, ORIGINZ);
	m_upperRight = Point3f(UPPERRIGHTX, UPPERRIGHTY, UPPERRIGHTZ);
	m_lowerLeft = Point3f(LOWERLEFTX, LOWERLEFTY, LOWERLEFTZ);
	UpdateBasis();
}

void PhoneCalibration::WriteToFile()
{
	ofstream outfile;
	outfile.open(CALIB_FILE, ios::out | ios::trunc);
	char buffer[128];
	sprintf(buffer, "%.4f\n%.4f\n%.4f\n%.4f\n%.4f\n%.4f\n%.4f\n%.4f\n%.4f", m_origin.x, m_origin.y, m_origin.z,
		m_upperRight.x, m_upperRight.y, m_upperRight.z,
		m_lowerLeft.x, m_lowerLeft.y, m_lowerLeft.z);
	outfile << buffer;
	outfile.close();
	cout << "wrote phone calibration to " << CALIB_FILE << endl;
}

void PhoneCalibration::InitFromFile()
{
	ifstream infile;
	cout << "Reading phone calibration from " << CALIB_FILE << endl;
	if(!infile)
	{
		cerr<< "ERROR: calibration file " << CALIB_FILE << " does not exist, using pre-defined values" << endl;
		return;
	}
	infile.open(CALIB_FILE, ios::in);
	vector<float> vals;
	string line;
	while(infile.good())
	{
		getline(infile, line);
		vals.push_back(atof(line.c_str()));
	}

	if(vals.size() != 9)
	{
		cerr<< "ERROR: invalid calibration file, using predefined values"<<endl;
		return;
	} 

	// todo: make this safe!!!
	m_origin = Point3f( vals[0] , vals[1] , vals[2] );
	m_upperRight = Point3f(vals[3] , vals[4] , vals[5] );
	m_lowerLeft = Point3f(vals[6] , vals[7] , vals[8] );
}

PhoneCalibration::~PhoneCalibration(void){}

void PhoneCalibration::UpdateBasis()
{
	m_unitX = m_upperRight - m_origin;
	m_unitZ = m_lowerLeft - m_origin;
	m_unitY = m_unitX.cross(m_unitZ);

	m_xLength = norm(m_unitX);
	m_yLength = norm(m_unitY);
	m_zLength = norm(m_unitZ);

	m_unitX = m_unitX / norm(m_unitX);
	m_unitY = m_unitY / norm(m_unitY);
	m_unitZ = m_unitZ / norm(m_unitZ);
}

Point3f PhoneCalibration::ToPhoneSpaceAsPercentage(Point3f coord)
{
	if(coord.x == PMD_INVALID_DISTANCE)
		return Point3f(PMD_INVALID_DISTANCE, PMD_INVALID_DISTANCE, PMD_INVALID_DISTANCE);
	Point3f tmp = coord - m_origin;
	double x = tmp.dot(m_unitX);
	double y = tmp.dot(m_unitY);
	double z = tmp.dot(m_unitZ);

	return Point3f(x / m_xLength,y,z / m_zLength);
}

Point3f PhoneCalibration::ToPhoneSpace(Point3f coord)
{
	if(coord.x == PMD_INVALID_DISTANCE)
		return Point3f(PMD_INVALID_DISTANCE, PMD_INVALID_DISTANCE, PMD_INVALID_DISTANCE);
	Point3f tmp = coord - m_origin;
	double x = tmp.dot(m_unitX);
	double y = tmp.dot(m_unitY);
	double z = tmp.dot(m_unitZ);

	return Point3f(x,y,z);
}

void PhoneCalibration::ToPhoneSpace(float* src, float* dst)
{
	float* pSrc = src;
	float* pDst = dst;

	float ox = m_origin.x;
	float oy = m_origin.y;
	float oz = m_origin.z;

	float xx = m_unitX[0];
	float xy = m_unitX[1];
	float xz = m_unitX[2];

	float yx = m_unitY[0];
	float yy = m_unitY[1];
	float yz = m_unitY[2];

	float zx = m_unitZ[0];
	float zy = m_unitZ[1];
	float zz = m_unitZ[2];

	for(int i = 0; i < PMDIMAGESIZE; i++, pSrc += 3, pDst +=3)
	{
		float x = pSrc[0] - ox;
		float y = pSrc[1] - oy;
		float z = pSrc[2] - oz;

		pDst[0] = xx * x + xy * y + xz * z;
		pDst[1] = yx * x + yy * y + yz * z;
		pDst[2] = zx * x + zy * y + zz * z;
	}
}



