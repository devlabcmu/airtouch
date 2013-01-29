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
	IplImage* GetCvDistances();

	void Threshold(float maxdistance);
	
	// Getters
	PMDFingerData const* const GetFingerData(){ return &m_pmdFingerData;}
	float const* const GetDistanceBuffer() {return m_pmdDistanceBuffer;}
	IplImage const* const GetDistancesProcessed() {return m_pmdDistancesProcessed;}
	BackgroundSubtractionData const* const GetBackgroundSubtractionData() {return &m_backgroundSubtractionData;}

	// Renders the distance values to the given opencv image
	void RenderDistances(const IplImage* src, IplImage* renderTo);
	void RenderDistances(const float * src, IplImage* renderTo);

	// Image Processing
	void MedianFilter();
	void UpdateBackgroundSubtraction();
	void UpdateFingers();

	// static methods
	static void DepthDataToImage(float const* pDepthData, unsigned char* imgPtr, int rowStep, int step);


private:
	PMDDataDescription m_pmdDataDescription;
	PMDHandle m_pmdHandle;
	
	char m_pmdErrorBuffer[BUFSIZE];

	// Data
	unsigned int m_pmdFlags[PMDIMAGESIZE];
	float m_pmdDistanceBuffer[PMDIMAGESIZE];
	IplImage* m_pmdDistancesProcessed;
	
	// Image Processing
	BackgroundSubtractionData m_backgroundSubtractionData;

	PMDFingerData m_pmdFingerData;

	// OpenCV UI
	
};

