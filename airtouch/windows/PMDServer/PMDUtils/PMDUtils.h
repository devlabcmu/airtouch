#pragma once
#include <Windows.h>
#include <iostream>

#include <opencv/cxcore.h>
#include <opencv/highgui.h>
#include <opencv2/imgproc/imgproc.hpp>

#include "PMDConstants.h"

class PMDUtils
{
public:
	static void DistancesToImage(float const* pDepthData, unsigned char* imgPtr, int rowStep, int step);
	static void AmplitudesToImage(float const* pDepthData, unsigned char* imgPtr, int rowStep, int step);
	static void DistancesToImage(IplImage const* distances, IplImage* out) { DistancesToImage((const float*) distances->imageData, out);}
	static void DistancesToImage(float const* pDistances, IplImage* out) { DistancesToImage(pDistances, (unsigned char *) out->imageData, out->widthStep / sizeof(unsigned char), out->nChannels); }
	static void AmplitudesToImage(float const* pAmplitudes, IplImage* out) { AmplitudesToImage(pAmplitudes, (unsigned char *) out->imageData, out->widthStep / sizeof(unsigned char), out->nChannels); }
};

