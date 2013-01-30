#include "PhoneCalibration.h"
#include "PMDConstants.h"

#define PHONE_ANDROID

#ifdef PHONE_ANDROID
#define ORIGINX 0.0387f
#define ORIGINY -0.0329f
#define ORIGINZ 0.0644

#define P1X -0.0169f
#define P1Y -0.0271f
#define P1Z 0.0548f

#define P2X 0.0392f
#define P2Y -0.0196f
#define P2Z  0.2121f
#endif

#ifdef PHONE_IPHONE
// calibration for iphone
#define ORIGINX 0.0387f
#define ORIGINY -0.0329f
#define ORIGINZ 0.0644

#define P1X -0.0169f
#define P1Y -0.0271f
#define P1Z 0.0548f

#define P2X 0.0392f
#define P2Y -0.0196f
#define P2Z  0.2121f
#endif

PhoneCalibration::PhoneCalibration(void)
{
	m_origin = (Mat_<float>(3,1,IPL_DEPTH_32F) << ORIGINX , ORIGINY, ORIGINZ);
	m_unitX = (Mat_<float>(3,1,IPL_DEPTH_32F) << P1X - ORIGINX , P1Y - ORIGINY, P1Z - ORIGINZ);
	m_unitZ = (Mat_<float>(3,1,IPL_DEPTH_32F) << P2X - ORIGINX , P2Y - ORIGINY, P2Z - ORIGINZ);
	m_unitY = m_unitX.cross(m_unitZ);

	m_unitX = m_unitX / norm(m_unitX);
	m_unitY = m_unitY / norm(m_unitY);
	m_unitZ = m_unitZ / norm(m_unitZ);
}


PhoneCalibration::~PhoneCalibration(void){}

Point3f PhoneCalibration::ToPhoneSpace(Point3f coord)
{
	Mat tmp = Mat(coord) - m_origin;
	double x = tmp.dot(m_unitX);
	double y = tmp.dot(m_unitY);
	double z = tmp.dot(m_unitZ);

	return Point3f(x,y,z);
}

void PhoneCalibration::ToPhoneSpace(float* src, float* dst)
{
	float* pSrc = src;
	float* pDst = dst;

	float ox = m_origin.at<float>(0);
	float oy = m_origin.at<float>(1);
	float oz = m_origin.at<float>(2);

	float xx = m_unitX.at<float>(0);
	float xy = m_unitX.at<float>(1);
	float xz = m_unitX.at<float>(2);

	float yx = m_unitY.at<float>(0);
	float yy = m_unitY.at<float>(1);
	float yz = m_unitY.at<float>(2);

	float zx = m_unitZ.at<float>(0);
	float zy = m_unitZ.at<float>(1);
	float zz = m_unitZ.at<float>(2);

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



const float g_RBTrx = -1.390765f;
const float g_RBTry = -0.2451665f;
const float g_RBTrz = 0.2061744f;

const float g_RBTtx=0.0570102f;
const float g_RBTty=-0.04146893f;
const float g_RBTtz=-0.04173907f;

RBTPhoneCalibration::RBTPhoneCalibration()
{
	Mat rvec = (Mat_<float>(3,1,IPL_DEPTH_32F) << g_RBTrx , g_RBTry, g_RBTrz);
	// compute rotation matrix from rvec
	Rodrigues(rvec, m_rotationMatrix);
	m_translation = (Mat_<float>(3,1,IPL_DEPTH_32F) << g_RBTtx, g_RBTty ,g_RBTtz);

}

Point3f RBTPhoneCalibration::ToPhoneSpace(Point3f coord)
{
	Mat src = Mat(coord);
	Mat dst = m_rotationMatrix * src;
	Mat result =  dst + m_translation;
	return Point3f(result.at<float>(0),result.at<float>(1),result.at<float>(2)); 
}


