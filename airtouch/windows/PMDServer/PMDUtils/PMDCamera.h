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

	// Initialization
	HRESULT InitializeCamera();
	HRESULT InitializeCameraFromFile(char* filename);
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
			if(i >= m_newFingers.size()) break;
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
	vector<KeyPoint> GetBlobPoints() {return m_blobPoints;}
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

	// Image Processing
	void MedianFilter();
	void UpdateBackgroundSubtraction();
	void UpdateFingers();
	void RemoveReflection();
	void Threshold(float maxdistance);
	void Erode(int erosionSize);
	void FindBlobs();

private:
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
	PMDFingerData m_pmdFingerData;


	// Image Processing
	BackgroundSubtractionData m_backgroundSubtractionData;
	Ptr<FeatureDetector> m_blobDetector;
	vector<KeyPoint> m_blobPoints;

	// Calibration
	PhoneCalibration m_phoneCalibration;

	

	// FInger Tracking
	vector<Finger> m_oldFingers;
	vector<Finger> m_newFingers;
	void BlobsToFingers();
	void UpdateFingerPositions();
	static bool blobCompare(KeyPoint a, KeyPoint b) { return a.size > b.size;}
};

