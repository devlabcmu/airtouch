#pragma once
#include <Windows.h>
#include <iostream>

#include <opencv/cxcore.h>
#include <opencv/highgui.h>
#include <opencv2/imgproc/imgproc.hpp>

#include <pmdsdk2.h>
#include "PMDConstants.h"
#include "pmddata.h"


#define BUFSIZE 512
#define BACKGROUND_THRESHOLD_STDEV 3

using namespace cv;
using namespace std;

typedef struct {
	float means[PMDIMAGESIZE];
	float stdevs[PMDIMAGESIZE];
} BackgroundSubtractionData;


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
	PMDFingerData const* const GetFingerData(){ return &m_pmdFingerData;}
	float const* const GetDistanceBuffer() {return m_pmdDistanceBuffer;}
	float const* const GetIntensitiesBuffer() {return m_pmdIntensitiesBuffer;}
	UINT const* const GetFlags(){return m_pmdFlags;}
	IplImage const* const GetCoords() {return m_pmdCoords;}
	IplImage const* const GetDistancesProcessed() {return m_pmdDistancesProcessed;}
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
	void Threshold(float maxdistance);

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
	
	// Image Processing
	BackgroundSubtractionData m_backgroundSubtractionData;

	PMDFingerData m_pmdFingerData;

	// OpenCV UI
	
};

