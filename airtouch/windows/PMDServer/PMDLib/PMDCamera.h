#pragma once
#include <Windows.h>
#include <iostream>

#include <opencv/cxcore.h>
#include <opencv/highgui.h>
#include <opencv2/imgproc/imgproc.hpp>

#include <pmdsdk2.h>
#include "PMDConstants.h"
#include "pmddata.h"
#include "PhoneCalibration.h"
#include "PMDUtils.h"

#define BUFSIZE 512
#define BACKGROUND_THRESHOLD_STDEV 3

using namespace cv;
using namespace std;

enum PMDFingerTrackingMode
{
	FINGER_TRACKING_INTERPOLATE_CLOSEST = 0,
	FINGER_TRACKING_INTERPOLATE_BRIGHTEST,
	FINGER_TRACKING_BRIGHTEST
};

typedef struct {
	float means[PMDIMAGESIZE];
	float stdevs[PMDIMAGESIZE];
} BackgroundSubtractionData;

typedef struct {
	Point2f screenCoords;
	Point2f blobCenter;
	float blobSize;
	Point3f worldCoords;
	Point3f phoneCoords;
	int id;
} Finger;

typedef struct {
	Point2f pt;
	float size;
} BlobPoint;

typedef struct {
	Point2f blobPoint;
	Point2f fingerPoint;
	int blobIndex;
	int fingerIndex;
	float distance;
} ClosestBlobInfo;

class PMDCamera
{
public:
	PMDCamera(void);
	~PMDCamera(void);

	// public vars
	PMDFingerTrackingMode FingerTrackingMode;


	// Initialization
	HRESULT InitializeCamera();
	HRESULT InitializeCameraFromFile(const char* filename);
	HRESULT UpdateCameraData();
	HRESULT InitializeBackgroundSubtraction();


	// OpenCV UI
	IplImage* GetCvBackgroundImage();


	// Getters
	PMDFingerData const const GetFingerData()
	{ 
		PMDFingerData result;
		ZeroMemory(&result, sizeof(result));
		float* pR = (float*)&result;
		// copy up to MAX_FINGERS fingers worth of data
		for(int i = 0; i < 2; i++)
		{
			if(i >= m_newFingers.size()) 
			{
				pR[0] = PMD_INVALID_DISTANCE;
				pR[1] = PMD_INVALID_DISTANCE;
				pR[2] = PMD_INVALID_DISTANCE;
				continue;
			}
			Point3f p = m_newFingers[i].phoneCoords;
			pR[0] = p.x;
			pR[1] = p.y;
			pR[2] = p.z;
			pR+=3;
		}
		return result;
	}
	float const* const GetDistanceBuffer() {return m_pmdDistanceBuffer;}
	float const* const GetIntensitiesBuffer() {return m_pmdIntensitiesBuffer;}
	UINT const* const GetFlags(){return m_pmdFlags;}
	IplImage const* const GetCoords() {return m_pmdCoords;}
	IplImage const* const GetDistancesProcessed() {return m_pmdDistancesProcessed;}
	IplImage const* const GetDistancesProcessedRGB() {return m_pmdDistancesProcessedRGB;}
	IplImage const* const GetCoordsPhoneSpace() { return m_pmdPhoneSpace; }
	char const* const GetFingerIdMask() {return m_fingerIdMask;}
	vector<BlobPoint> GetBlobPoints() {return m_blobPoints;}
	vector<BlobPoint> GetBlobPointsIntensities() {return m_blobPointsIntensity;}
	vector<Finger> GetFingers() {return m_newFingers;}
	BackgroundSubtractionData const* const GetBackgroundSubtractionData() {return &m_backgroundSubtractionData;}

	Point3f GetCoord(int row, int col)
	{ 
		Point3f result;
		result.x=0;
		result.y=0;
		result.z=0;

		if(m_pmdFlags[row * PMDNUMCOLS + col] & PMD_FLAG_INVALID) return result;
		float* pCoords = (float*)m_pmdCoords->imageData;
		int idx = (row * PMDNUMCOLS + col) * 3;
		result.x=pCoords[idx];
		result.y=pCoords[idx + 1];
		result.z=pCoords[idx + 2];
		return result;
	};



	// Finger tracking
	void UpdateFingers();


private:
	// flags
	bool m_blobIntensitiesFound;

	// methods


	PMDDataDescription m_pmdDataDescription;
	PMDHandle m_pmdHandle;

	char m_pmdErrorBuffer[BUFSIZE];

	// Data
	unsigned int m_pmdFlags[PMDIMAGESIZE];
	float m_pmdDistanceBuffer[PMDIMAGESIZE];
	float m_pmdIntensitiesBuffer[PMDIMAGESIZE];
	IplImage* m_pmdCoords;
	IplImage* m_pmdDistancesProcessed;
	IplImage* m_pmdDistancesProcessedRGB;
	IplImage* m_pmdPhoneSpace;
	IplImage* m_pmdIntensitiesRGB;
	PMDFingerData m_pmdFingerData;

	BackgroundSubtractionData m_backgroundSubtractionData;
	Ptr<FeatureDetector> m_blobDetector;
	Ptr<FeatureDetector> m_intensitiesBlobDetector;

	vector<BlobPoint> m_blobPoints;
	vector<BlobPoint> m_blobPointsIntensity;

	PhoneCalibration m_phoneCalibration;

	vector<Finger> m_oldFingers;
	vector<Finger> m_newFingers;
	char m_fingerIdMask[PMDIMAGESIZE];

	// Image Processing
	void MedianFilter();
	void UpdateBackgroundSubtraction();
	void RemoveReflection();
	void RemoveOutsidePhone();
	void Threshold(float maxdistance);
	void Erode(int erosionSize);

	// Blob Tracking
	void FindConnectedComponentsInDistanceImage();
	void BlobsToFingers();
	void FindBlobsInDistanceImage();
	bool validPoint(Point2i pt);
	void UpdateFingerIdMask();
	void FindBlobsInIntensityImage();

	// Finger Tracking
	void UpdateFingerPositions();
	// Gets the position of fingers in screen space based
	// on the FingerTrackingMode variable.
	Point2f GetFingerPositionScreenSpace(vector<Finger>::iterator f, bool newFinger);
	// Finds the finger position using information about the blob
	// Assumes user is wearing an IR reflective marker
	// Returns the 2d position in screen space
	Point2f FindFingerPosBrightest(vector<Finger>::iterator f, bool newFinger);
	// Finds the finger position using information about the blob
	// Returns the 2d position in screen space
	Point2f FindFingerPosInterpolateClosest(vector<Finger>::iterator f, bool newFinger);

	Point2f FindFingerPosInterpolateBrightest(vector<Finger>::iterator f, bool newFinger);
	static bool blobCompare(BlobPoint a, BlobPoint b) { return a.size > b.size;}

};

