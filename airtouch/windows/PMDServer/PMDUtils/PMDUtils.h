#pragma once
#include <Windows.h>
#include <iostream>

#include <opencv/cxcore.h>
#include <opencv/highgui.h>
#include <opencv2/imgproc/imgproc.hpp>

#include "PMDConstants.h"
#include <pmdsdk2.h>

class PMDUtils
{
public:
	static void DistancesToImage(float const* pDepthData, unsigned char* imgPtr, int rowStep, int step);
	static void AmplitudesToImage(float const* pDepthData, unsigned char* imgPtr, int rowStep, int step);
	static void CoordsToImage(float const* pDepthData, UINT const* pFlags, unsigned char* imgPtr, int rowStep, int step);
	
	static void DistancesToImage(IplImage const* distances, IplImage* out) { DistancesToImage((const float*) distances->imageData, out);}
	static void CoordsToImage(IplImage const* coords, UINT const* pFlags, IplImage* out) { CoordsToImage((const float*) coords->imageData, pFlags, (unsigned char*) out->imageData, out->widthStep / sizeof(unsigned char), out->nChannels); }

	static void DistancesToImage(float const* pDistances, IplImage* out) { DistancesToImage(pDistances, (unsigned char *) out->imageData, out->widthStep / sizeof(unsigned char), out->nChannels); }
	static void AmplitudesToImage(float const* pAmplitudes, IplImage* out) { AmplitudesToImage(pAmplitudes, (unsigned char *) out->imageData, out->widthStep / sizeof(unsigned char), out->nChannels); }
};

