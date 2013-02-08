#include "PMDCamera.h"
#include <algorithm>
#include <queue>
#include "ConnectedComponents.h"
#include <unordered_map>
#include "strutils.h"
#include "opencvutils.h"

#define SOURCE_PLUGIN "camboardnano"
#define SOURCE_PARAM ""
#define PROC_PLUGIN "camboardnanoproc"
#define PROC_PARAM ""

#define FILE_SOURCE_PLUGIN "pmdfile"


const int g_numFramesForBackgroundSubtraction = 50;
const int g_minBlobSize = 200;
const int g_maxBlobSize = 1000000;
const float g_fingerScreenSmoothing = 0.0f;
const float g_fingerWorldSmoothing = 0.7f;
const float g_convexHullStdDevDistances = 0.018;
const float g_convexHullStDevCenterDst = 6;
const float g_orientationLength = 40;
PMDCamera::PMDCamera(void)
{
	FingerTrackingMode = FINGER_TRACKING_INTERPOLATE_CLOSEST;
	
	// create all opencv images

	m_pmdDistancesProcessed = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), IPL_DEPTH_32F, 1);
	m_pmdPhoneSpace = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), IPL_DEPTH_32F, 3);
	m_pmdCoords = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), IPL_DEPTH_32F, 3);
	m_pmdDistancesProcessedRGB = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), 8, 3); 
	m_pmdIntensitiesRGB = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), 8, 3);
	m_connectedComponents = Mat(PMDNUMROWS, PMDNUMCOLS, CV_8U);
	m_fingerIdMask = Mat(PMDNUMROWS, PMDNUMCOLS, CV_8S);

	SimpleBlobDetector::Params blobParams;
	blobParams.minThreshold = 1;
	blobParams.maxThreshold = 255;
	blobParams.thresholdStep = 100;
	
	blobParams.minDistBetweenBlobs = 1.0f;

	blobParams.filterByArea = true;
	blobParams.minArea = 200.0f;
	blobParams.maxArea = 100000.0f;

	blobParams.filterByCircularity = false;
	
	blobParams.filterByColor = true;
	blobParams.blobColor = 255;

	blobParams.filterByConvexity = false;
	blobParams.filterByInertia = false;
	

	m_blobDetector = new SimpleBlobDetector(blobParams);
	m_blobDetector->create("DistancesBlob");

	blobParams.minThreshold = 100;
	blobParams.maxThreshold = 255;
	blobParams.thresholdStep = 50;
	
	blobParams.minArea = 1.0f;
	blobParams.maxArea = 1000000.0f;
	m_intensitiesBlobDetector = new SimpleBlobDetector(blobParams);
	m_intensitiesBlobDetector->create("IntensitiesBlob");
}

PMDCamera::~PMDCamera(void)
{
	// release all opencv images
	// breaks
	cvReleaseImage(&m_pmdDistancesProcessed);
	cvReleaseImage(&m_pmdCoords);
	cvReleaseImage(&m_pmdPhoneSpace);
	cvReleaseImage(&m_pmdDistancesProcessedRGB);
	cvReleaseImage(&m_pmdIntensitiesRGB);
}

HRESULT PMDCamera::InitializeCamera()
{
	int res;

	res = pmdOpen (&m_pmdHandle, SOURCE_PLUGIN, SOURCE_PARAM, PROC_PLUGIN, PROC_PARAM);
	if (res != PMD_OK)
	{
		pmdGetLastError (0, m_pmdErrorBuffer, BUFSIZE);
		cout << "PMDCamera Error: Could not connect to pmd: " << m_pmdErrorBuffer << endl;
		return -1;
	}

	cout << "opened sensor" << endl;  

	return 0;
}

HRESULT PMDCamera::InitializeCameraFromFile(const char* filename)
{
	int res;

	res = pmdOpen (&m_pmdHandle, FILE_SOURCE_PLUGIN, filename , PROC_PLUGIN, PROC_PARAM);
	if (res != PMD_OK)
	{
		pmdGetLastError (0, m_pmdErrorBuffer, BUFSIZE);
		cout << "PMDCamera Error: Could not open file: " << m_pmdErrorBuffer << endl;
		return -1;
	}

	cout << "PMDCamera: opened sensor from file " << filename << endl;  

	return 0;
}

HRESULT PMDCamera::InitializeBackgroundSubtraction()
{
	// for first 50 frames compute mean, standard deviation
	ZeroMemory(m_backgroundSubtractionData.means, _countof(m_backgroundSubtractionData.means) * sizeof(float));
	ZeroMemory(m_backgroundSubtractionData.stdevs, _countof(m_backgroundSubtractionData.means) * sizeof(float));
	// first get 50 frames
	float* frames[g_numFramesForBackgroundSubtraction];
	float framesForAverage[PMDIMAGESIZE];
	ZeroMemory(framesForAverage, _countof(framesForAverage) * sizeof(float));

	// fill in the frames
	for (int i = 0; i < g_numFramesForBackgroundSubtraction; i++)
	{
		float* frame = (float*) malloc(sizeof(float) * PMDIMAGESIZE); // new float[PMDIMAGESIZE];
		frames[i] = frame;
		// fill the frame with data
		HRESULT hr = UpdateCameraData();

		if(!SUCCEEDED(hr))
		{
			fprintf(stderr, "Error: Failed to update camera data in initializeBackgroundSubtraction\n");
			return -1;
		}
		
		memcpy_s(frame, PMDIMAGESIZE * sizeof(float), m_pmdDistanceBuffer, PMDIMAGESIZE * sizeof(float));
	}

	// figure out how many frames to average over for each pixel (depends on valid data)
	for (int i = 0; i < g_numFramesForBackgroundSubtraction; i++)
	{
		for(int j = 0; j < PMDIMAGESIZE; j++)
		{
			if(frames[i][j] != PMD_INVALID_DISTANCE) framesForAverage[j]++;
		}
	}
	// get averages
	for (int i = 0; i < g_numFramesForBackgroundSubtraction; i++)
	{
		float* curFrame = frames[i];
		float* curBgRow = 0;
		float* curDataRow = 0;
		for(int y = 0; y < PMDNUMROWS; y++)
		{
			curBgRow = &m_backgroundSubtractionData.means[y* PMDNUMCOLS];
			curDataRow = &curFrame[y * PMDNUMCOLS];
			
			for(int x = 0; x < PMDNUMCOLS; x++)
			{
				if(curDataRow[x] == PMD_INVALID_DISTANCE) continue;
				int avI = y * PMDNUMCOLS + x;
				if(framesForAverage[avI] > 0)
					curBgRow[x] += curDataRow[x] / framesForAverage[avI];
				else
					curBgRow[x] = PMD_INVALID_DISTANCE;
			}
		}
	}
	
	// compute standard deviation for each frame
	for (int i = 0; i < g_numFramesForBackgroundSubtraction; i++)
	{
		float* frame = frames[i];
		
		// fill the frame with data
		float* curMeanRow = 0;
		float* curStdevRow = 0;
		float* curDataRow = 0;
		for(int y = 0; y < PMDNUMROWS; y++)
		{
			curMeanRow = &m_backgroundSubtractionData.means[y* PMDNUMCOLS];
			curStdevRow = &m_backgroundSubtractionData.stdevs[y * PMDNUMCOLS];
			curDataRow = &frame[y * PMDNUMCOLS];
			for(int x = 0; x < PMDNUMCOLS; x++)
			{
				if(curDataRow[x] == PMD_INVALID_DISTANCE) continue;
				int avI = y * PMDNUMCOLS + x;
				if(framesForAverage[avI] > 0)
					curStdevRow[x] += pow(curDataRow[x] - curMeanRow[x], 2) / framesForAverage[avI];
				else
					curStdevRow[x] = PMD_INVALID_DISTANCE;
			}
		}
	}
	for (int i = 0; i < PMDIMAGESIZE; i++)
	{
		if(m_backgroundSubtractionData.stdevs[i] > 0)
			m_backgroundSubtractionData.stdevs[i] = sqrt(m_backgroundSubtractionData.stdevs[i]);
	}

	// free all the frames
	for (int i = 0; i < g_numFramesForBackgroundSubtraction; i++)
	{
		free(frames[i]);
	}
	return 0;
}

HRESULT PMDCamera::UpdateCameraData()
{
	int res = pmdUpdate (m_pmdHandle);
	if (res != PMD_OK)
	{
		pmdGetLastError (m_pmdHandle, m_pmdErrorBuffer, 128);
		fprintf (stderr, "UpdateCameraData Error: Could not update data.\n", m_pmdErrorBuffer);
		pmdClose (m_pmdHandle);
		return -1;
	}

	res = pmdGetSourceDataDescription (m_pmdHandle, &m_pmdDataDescription);
	if (res != PMD_OK)
	{
		pmdGetLastError (m_pmdHandle, m_pmdErrorBuffer, BUFSIZ);
		fprintf (stderr, "UpdateCameraData Error: Could not get data description: %s\n", m_pmdErrorBuffer);
		pmdClose (m_pmdHandle);
		return -1;
	}

	if (m_pmdDataDescription.subHeaderType != PMD_IMAGE_DATA)
	{
		fprintf (stderr, "UpdateCameraData Error: Source data is not an image!\n");
		pmdClose (m_pmdHandle);
		return -1;
	}

	// make sure that the numrows and numcolumsn is same size as PMDIMAGESIZE
	assert(m_pmdDataDescription.img.numColumns * m_pmdDataDescription.img.numRows == PMDIMAGESIZE);

	// todo: this shoudl be copied to a seperate location...
	res = pmdGetDistances (m_pmdHandle, m_pmdDistanceBuffer, PMDIMAGESIZE * sizeof (float));
	if (res != PMD_OK)
	{
		pmdGetLastError (m_pmdHandle, m_pmdErrorBuffer, 128);
		fprintf (stderr, "UpdateCameraData Error: Could not get distances: %s\n", m_pmdErrorBuffer);
		pmdClose (m_pmdHandle);
		return -1;
	}

	res = pmdGetAmplitudes (m_pmdHandle, m_pmdIntensitiesBuffer, PMDIMAGESIZE * sizeof (float));
	if (res != PMD_OK)
	{
		pmdGetLastError (m_pmdHandle, m_pmdErrorBuffer, 128);
		fprintf (stderr, "UpdateCameraData Error: Could not get intensities: %s\n", m_pmdErrorBuffer);
		pmdClose (m_pmdHandle);
		return -1;
	}

	res = pmdGet3DCoordinates (m_pmdHandle, (float *)m_pmdCoords->imageData, PMDIMAGESIZE * 3 * sizeof (float));
	if (res != PMD_OK)
	{
		pmdGetLastError (m_pmdHandle, m_pmdErrorBuffer, 128);
		fprintf (stderr, "UpdateCameraData Error: Could not get 3d coordinates: %s\n", m_pmdErrorBuffer);
		pmdClose (m_pmdHandle);
		return -1;
	}

	res = pmdGetFlags(m_pmdHandle, m_pmdFlags, PMDIMAGESIZE * 4);
	if (res != PMD_OK)
	{
		pmdGetLastError (m_pmdHandle, m_pmdErrorBuffer, 128);
		fprintf (stderr, "UpdateCameraData Error: Could not get flags: %s\n", m_pmdErrorBuffer);
		pmdClose (m_pmdHandle);
		return -1;
	}

	// Zero out all invalid coordinates
	for (int i = 0; i < PMDIMAGESIZE; i++)
	{
		if(m_pmdFlags[i] & PMD_FLAG_INVALID)
		{
			m_pmdDistanceBuffer[i] = PMD_INVALID_DISTANCE;
		}
	}

	// copy the raw data to the processed data
	memcpy_s(m_pmdDistancesProcessed->imageData, PMDIMAGESIZE * sizeof(float), m_pmdDistanceBuffer, PMDIMAGESIZE * sizeof(float));

	// get the coordinates in phone space
	m_phoneCalibration.ToPhoneSpace((float*)m_pmdCoords->imageData, (float*)m_pmdPhoneSpace->imageData);


	// update falgs
	m_blobIntensitiesFound = false;

	return 0;
}

void PMDCamera::Threshold(float maxDistance)
{
	float* pDepthData = (float*)m_pmdDistancesProcessed->imageData;
	for (int y = 0; y < PMDNUMROWS; ++y)
    {
		for (int x = 0; x < PMDNUMCOLS; ++x, ++pDepthData)
        {
			if(*pDepthData > maxDistance) *pDepthData = PMD_INVALID_DISTANCE;
		}
	}
}

void PMDCamera::UpdateBackgroundSubtraction()
{
	// in the depthbuffer, make anything that is within stdev of mean -1
	float* pDistances = (float*)m_pmdDistancesProcessed->imageData;

	for (int i = 0; i < PMDIMAGESIZE; i++)
	{
		if(pDistances[i] == PMD_INVALID_DISTANCE) continue;
		if(m_backgroundSubtractionData.means[i] == PMD_INVALID_DISTANCE) continue;
		if(abs(pDistances[i]  - m_backgroundSubtractionData.means[i]) < BACKGROUND_THRESHOLD_STDEV * m_backgroundSubtractionData.stdevs[i])
		{
			pDistances[i] = PMD_INVALID_DISTANCE;
		}
	}
}


//***************************************
// Add new finger tracking algorithm here
//***************************************

Point2f PMDCamera::FindFingerPosInterpolateClosest(vector<Finger>::iterator f, bool newFinger)
{
	float* pDistances = (float*) m_pmdDistancesProcessed->imageData;
	int x, y;
	float minZ = 1000.0f;
	float minX = 0, minY = 0;
	int searchSize = newFinger ? f->blobSize : 20;
	bool first = true;
	for(int dy = -searchSize; dy < searchSize; dy++)
	{
		y = (newFinger ? f->blobCenter.y : f->screenCoords.y) + dy;
		if(y < 0 || y > PMDNUMROWS || abs(y - f->blobCenter.y) > f->blobSize * 1.5) continue;
		for(int dx = -searchSize; dx < searchSize; dx++)
		{
			x = (newFinger ? f->blobCenter.x : f->screenCoords.x) + dx;
			if(first)
			{
				minX = x;
				minY = y;
				first = false;
			}
			if(x < 0 || x > PMDNUMCOLS || abs(x - f->blobCenter.x) > f->blobSize * 1.5) continue;
			int idx = y * PMDNUMCOLS + x;
			if(m_pmdFlags[idx] & PMD_FLAG_INVALID) continue;
			float dst = pDistances[idx];
			if(dst == PMD_INVALID_DISTANCE) continue;
			if(dst < minZ)
			{
				minX = x;
				minY = y;
				minZ = dst;
			}
		}
	}
	return Point2f(minX, minY);
}

Point2f PMDCamera::FindFingerPosInterpolateBrightest(vector<Finger>::iterator f, bool newFinger)
{
	Point2f result(0,0);
	
	// make a copy of the intensities
	float amplitudesCopy [PMDIMAGESIZE];
		
	memcpy(amplitudesCopy, m_pmdIntensitiesBuffer, PMDIMAGESIZE * sizeof(float));
	// zero out everything that's not in the finger
	// multiply amplitudes by distance to finger center
	float maxdst = sqrt(pow((float)PMDNUMCOLS,2)+pow((float)PMDNUMROWS,2));

	uchar* pFingerIdMask = m_fingerIdMask.ptr();
	for (int i = 0; i < PMDIMAGESIZE; i++)
	{
		if(pFingerIdMask[i] != f->id){
			amplitudesCopy[i] = 0;
			continue;
		}
		// if this is new finger, don't multiply by previous position
		if(newFinger) continue;
		float y = i / PMDNUMCOLS;
		float x = i % PMDNUMCOLS;
		float dx = x - f->screenCoords.x;
		float dy = y - f->screenCoords.y;
		float dst = sqrt(pow(dx,2) + pow(dy, 2));
		// normalize by the size of the image
		dst /= maxdst;
		dst = 1 - dst;
		dst = pow(dst,5);
		amplitudesCopy[i] *= dst;
	}

	// find all blobs in this image
	vector<KeyPoint> blobsInSubImage;
	PMDUtils::AmplitudesToImage(amplitudesCopy, m_pmdIntensitiesRGB);
	m_intensitiesBlobDetector->detect(m_pmdIntensitiesRGB, blobsInSubImage);

	// for each blob in the intensity image
	for(vector<KeyPoint>::iterator j = blobsInSubImage.begin(); j < blobsInSubImage.end(); j++)
	{
		// do everything per  blob
		int idx = (int)j->pt.y * PMDNUMCOLS + (int)j->pt.x;
		if(pFingerIdMask[idx] != f->id) continue;
		// if the blob's center is in the finger's mask
		result = j->pt;
		break;
	}

	return result;

}

Point2f PMDCamera::FindFingerPosContours(vector<Finger>::iterator f, bool newFinger)
{
	// Get mask of just finger
	int blobid = f->blobId;
	Mat fingerMask;
	bandpass(m_connectedComponents, fingerMask, blobid, blobid, 1);

	Mat canny_output;
	vector<vector<Point> > contours;
	vector<Vec4i> hierarchy;
	/// Detect edges using canny
	Canny( fingerMask, canny_output, 0, 1, 3 );
	
	/// Find contour and hull (should only be 1)
	findContours( canny_output, contours, hierarchy, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE, Point(0, 0) );

	int maxContourIndex = 0;
	int maxContourSize = 0;
	// keep only the biggest contour
	for(vector<vector<Point>>::iterator i = contours.begin(); i != contours.end(); i++)
	{
		if(i->size() > maxContourSize)
		{
			maxContourIndex = i - contours.begin();
			maxContourSize = i->size();
		}
	}

	vector<Point>hull;

	convexHull(Mat(contours[maxContourIndex]), hull, false);

	Scalar color = Scalar( 0, 0, 255 );
	
	vector<vector<Point>> hullWrapper;
	hullWrapper.push_back(hull);

	Vec2f orientation = Vec2f(f->screenCoords - f->blobCenter);

	// if the hull is too round, then just use brightest point interpolation
	float meanDst = 0;
	for (int i = 0; i < hull.size(); i++)
	{
		meanDst += norm(Point2f(hull[i]) - f->blobCenter);
	}
	meanDst /= hull.size();
	float stDevDst = 0;
	for (int i = 0; i < hull.size(); i++)
	{
		stDevDst += pow(norm(Point2f(hull[i]) - f->blobCenter) - meanDst,2);
	}
	stDevDst /= hull.size();
	stDevDst = sqrt(stDevDst);
	

	vector<HullInfo> hullInfo;
	for (int i = 0; i < hull.size(); i++)
	{
		HullInfo hi;
		Vec2f v1 = Vec2f(Point2f(hull[i]) - f->blobCenter);
		if(v1.dot(orientation) < 0) continue;
		hi.pt = hull[i];
		hi.dstBlobCenter = norm(Point2f(hull[i]) - f->blobCenter);
		hi.dstFingerCenter = norm(Point2f(hull[i]) - f->screenCoords);
		hullInfo.push_back(hi);
	}
	sort(hullInfo.begin(), hullInfo.end(), HullInfoCompareBlobDst);
	int topN = 3;
	if(hullInfo.size() > topN)
		hullInfo.erase(hullInfo.begin() + topN, hullInfo.end());
	sort(hullInfo.begin(), hullInfo.end(), HullInfoCompareFingerDst);

	if(hullInfo.size() <= 0) return FindFingerPosInterpolateBrightest(f, newFinger);

	if(!newFinger && norm(f->blobCenter - f->screenCoords) > g_orientationLength)
		return hullInfo[0].pt;

	if(f->stDevDistances  > g_convexHullStDevCenterDst)
		return hullInfo[0].pt;
	if(f->stDevDistances  > g_convexHullStdDevDistances)
		return FindFingerPosInterpolateBrightest(f, newFinger);
	return hullInfo[0].pt;
}

Point2f PMDCamera::FindFingerPosBrightest(vector<Finger>::iterator f, bool newFinger)
{
	Point2f result(0,0);
	
	// make a copy of the intensities
	float amplitudesCopy [PMDIMAGESIZE];
		
	memcpy(amplitudesCopy, m_pmdIntensitiesBuffer, PMDIMAGESIZE * sizeof(float));
	uchar* pFingerIdMask = m_fingerIdMask.ptr();
	// zero out everything that's not in the finger
	for (int i = 0; i < PMDIMAGESIZE; i++)
	{
		if(pFingerIdMask[i] != f->id) amplitudesCopy[i] = 0;
	}

	// find all blobs in this image
	vector<KeyPoint> blobsInSubImage;
	PMDUtils::AmplitudesToImage(amplitudesCopy, m_pmdIntensitiesRGB);
	m_intensitiesBlobDetector->detect(m_pmdIntensitiesRGB, blobsInSubImage);

	// for each blob in the intensity image
	for(vector<KeyPoint>::iterator j = blobsInSubImage.begin(); j < blobsInSubImage.end(); j++)
	{
		// do everything per  blob
		int idx = (int)j->pt.y * PMDNUMCOLS + (int)j->pt.x;
		if(pFingerIdMask[idx] != f->id) continue;
		// if the blob's center is in the finger's mask
		result = j->pt;

		break;
	}

	return result;
}

// Add code here to add new finger tracking algorithms
Point2f PMDCamera::GetFingerPositionScreenSpace(vector<Finger>::iterator f, bool newFinger)
{
	switch(FingerTrackingMode)
	{
		case FINGER_TRACKING_BRIGHTEST:
			return FindFingerPosBrightest(f, newFinger);
		case FINGER_TRACKING_INTERPOLATE_BRIGHTEST:
			return FindFingerPosInterpolateBrightest(f, newFinger);
		case FINGER_TRACKING_INTERPOLATE_CLOSEST:
			return FindFingerPosInterpolateClosest(f, newFinger);
		case FINGER_TRACKING_CONTOURS:
			return FindFingerPosContours(f, newFinger);
	}
}



//***************************************
// End Finger Tracking Algorithms
//***************************************

// helper for UpdateFingerIdMask
bool PMDCamera::validPoint(Point2i pt)
{
	int idx = pt.y * PMDNUMCOLS + pt.x;
	float* pDistancesProcessed = (float*)m_pmdDistancesProcessed->imageData;
	return pDistancesProcessed[idx] != PMD_INVALID_DISTANCE;
}

// call after blobstofingers but before findfingerpositions
void PMDCamera::UpdateFingerIdMask()
{
	Mat mask;
	bandpass(m_connectedComponents, mask, 0,0,1);
	m_fingerIdMask.setTo(Scalar(-1), mask);
	for (vector<Finger>::iterator i = m_newFingers.begin(); i < m_newFingers.end(); i++)
	{
		int id = i->blobId;
		bandpass(m_connectedComponents, mask, id, id, 1);
		m_fingerIdMask.setTo(Scalar(i->id), mask);
	}
}

void PMDCamera::FindConnectedComponentsInDistanceImage()
{
	// threshold the current processed image
	Mat thresholded;
	threshold(Mat(m_pmdDistancesProcessed), thresholded, 0.0, 1.0, 0);
	
	// find the connected components, output to the matrix
	m_nLabels = connectedComponents(m_connectedComponents, thresholded);
}

void PMDCamera::FindBlobsInDistanceImage()
{
	BlobPoint* blobPoints = new BlobPoint[m_nLabels];

	for(int i = 1; i < m_nLabels + 1; i++)
	{
		blobPoints[i-1].blobId = i;
		blobPoints[i-1].pt = Point2f();
		blobPoints[i-1].size = 0;
		blobPoints[i-1].meanDistance = 0;
		blobPoints[i-1].stDevDistances = 0;
	}
	// get center point and area
	float* pDistances = (float*)m_pmdDistancesProcessed->imageData;
	for(int row = 0; row < m_connectedComponents.rows; ++row) {
		uchar* p = m_connectedComponents.ptr(row);
		for(int col = 0; col < m_connectedComponents.cols; ++col, p++, pDistances++) {
			int id = *p;
			if(id == 0) continue;
			if(*pDistances == PMD_INVALID_DISTANCE) continue;
			blobPoints[id-1].size++;
			blobPoints[id-1].pt.x += col;
			blobPoints[id-1].pt.y += row;
			blobPoints[id-1].meanDistance += *pDistances;
		}
	}

	m_blobPoints.clear();
	for(int i = 0; i < m_nLabels; i++)
	{
		// skip over label 0, this is always the background
		if(blobPoints[i].size > g_maxBlobSize || blobPoints[i].size < g_minBlobSize) continue;
		blobPoints[i].pt.x /= blobPoints[i].size;
		blobPoints[i].pt.y /= blobPoints[i].size;
		blobPoints[i].meanDistance /= blobPoints[i].size;
		pDistances = (float*)m_pmdDistancesProcessed->imageData;
		for(int row = 0; row < m_connectedComponents.rows; ++row) {
			uchar* p = m_connectedComponents.ptr(row);
			for(int col = 0; col < m_connectedComponents.cols; ++col, p++, pDistances++) {
				if(*p-1 == i)
				blobPoints[i].stDevDistances += pow(*pDistances - blobPoints[i].meanDistance,2) ;
			}
		}
		blobPoints[i].stDevDistances /= blobPoints[i].size;
		blobPoints[i].stDevDistances = sqrt(blobPoints[i].stDevDistances);
		m_blobPoints.push_back(blobPoints[i]);
	}

	delete[] blobPoints;
}


void PMDCamera::BlobsToFingers()
{
	// reduce number of blobs to max_fingers
	vector<BlobPoint> blobCopy;
	for (vector<BlobPoint>::iterator i = m_blobPoints.begin(); i < m_blobPoints.end(); i++)
	{
		blobCopy.push_back(*i);
	}

	if(blobCopy.size() > PMD_MAX_FINGERS)
	{
		std::sort(blobCopy.begin(), blobCopy.end(), PMDCamera::blobCompare);
		blobCopy.erase(blobCopy.begin() + PMD_MAX_FINGERS - 1, blobCopy.end());
	}

	if(blobCopy.size() == 0) return;

	for(vector<BlobPoint>::iterator i = blobCopy.begin(); i !=blobCopy.end(); i++)
	{
		Finger newFinger;

		// find the finger that is closest to the current blob position
		int closestIndex=0;
		float closestDistance = 10000.0f;
		Point2f blobCenter = i->pt;
		for(vector<Finger>::iterator j = m_oldFingers.begin(); j !=m_oldFingers.end(); j++)
		{
			double dist = norm(blobCenter - j->blobCenter);
			if(dist < closestDistance)
			{
				closestDistance = dist;
				closestIndex = j - m_oldFingers.begin();
			}
		}

		bool fingerBlobMatch = m_oldFingers.size() > 0;
		// check if there are any other blobs that are closer to this finger
		for(vector<BlobPoint>::iterator k = blobCopy.begin(); k !=blobCopy.end() && fingerBlobMatch; k++)
		{
			if(k == i) continue;
			double dist = norm(k->pt - m_oldFingers[closestIndex].blobCenter);
			if(dist < closestDistance) fingerBlobMatch = false;
		}

		if(!fingerBlobMatch)
		{
			// we need to generate a new finger
			newFinger.id = 0;
			for(vector<Finger>::iterator j = m_newFingers.begin(); j !=m_newFingers.end(); j++)
			{
				if(newFinger.id <= j->id)
					newFinger.id = j->id + 1;
			}
			for(vector<Finger>::iterator j = m_oldFingers.begin(); j !=m_oldFingers.end(); j++)
			{
				if(newFinger.id <= j->id)
					newFinger.id = j->id + 1;
			}

			newFinger.screenCoords = cvPoint(-1,-1);
		} else
		{
			newFinger = m_oldFingers[closestIndex];

			// update the screen coordinates of the new finger, for now just blob coords
			// get rid of finger at that index
			m_oldFingers.erase(m_oldFingers.begin() + closestIndex);
		}
		newFinger.blobId = i->blobId;
		newFinger.blobCenter = i->pt;
		newFinger.blobSize = i->size;
		newFinger.stDevDistances = i->stDevDistances;
		newFinger.meanDistance = i->meanDistance;
		m_newFingers.push_back(newFinger);
	}

}

void PMDCamera::UpdateFingerPositions()
{
	float* pDistances = (float*) m_pmdDistancesProcessed->imageData;
	// for each finger, smooth with old screen position and new screen position
	// update rest of coordinates
	for(vector<Finger>::iterator j = m_newFingers.begin(); j !=m_newFingers.end(); j++)
	{
		// if the screen coordinates are invalid then the finger has just appeared
		bool newFinger = j->screenCoords.x < 0;
		
		// find the finger position in screen space using info about the blog
		Point2f fingerPos(0,0);

		// First get the finger posisiton in screen space, then we will smooth the world coordinates
		fingerPos = GetFingerPositionScreenSpace(j, newFinger);
	
		// smooth finger position screen space
		if(newFinger)
		{
			// if screen coords are new, just use those
			j->screenCoords = fingerPos;
		} else
		{
			// otherwise, smooth
			j->screenCoords.x = j->screenCoords.x * g_fingerScreenSmoothing + fingerPos.x * (1 - g_fingerScreenSmoothing);
			j->screenCoords.y = j->screenCoords.y * g_fingerScreenSmoothing + fingerPos.y * (1 - g_fingerScreenSmoothing);
		}

		// update world coords

		float* pWorld = (float*)m_pmdCoords->imageData;
		float* pDistancesProcessed = (float*)m_pmdDistancesProcessed->imageData;
		// get world coord by averaging over world coordinates in small region of finger
		int searchSize = 10;
		int numItems = 0;
		Point3f world(PMD_INVALID_DISTANCE,PMD_INVALID_DISTANCE,PMD_INVALID_DISTANCE);
		for(int dy = -searchSize; dy < searchSize; dy++)
		{
			int y = j->screenCoords.y + dy;
			if(y < 0 || y >= PMDNUMROWS) continue;
			for( int dx = -searchSize; dx < searchSize; dx++)
			{
				int x = j->screenCoords.x + dx;
				if(x < 0 || x >= PMDNUMCOLS) continue;
				int idx = y * PMDNUMCOLS + x;
				//if(m_fingerIdMask[idx] != j->id) continue;
				if(pDistancesProcessed[idx] == PMD_INVALID_DISTANCE) continue;
				if(world.x == PMD_INVALID_DISTANCE)
				{
					world.x = pWorld[3 * idx];
					world.y = pWorld[3 * idx + 1];
					world.z = pWorld[3 * idx + 2];
				}else
				{
					world.x += pWorld[3 * idx];
					world.y += pWorld[3 * idx + 1];
					world.z += pWorld[3 * idx + 2];
				}

				numItems++;
				
			}
		}
		// if the world coordinates are invalid (numItems == 0), don't update the finger
		if(numItems == 0) continue;

		// average out world position
		world.x = world.x * (1 / (float) numItems);
		world.y=  world.y * (1 / (float) numItems);
		world.z = world.z * (1 / (float) numItems);

		
		// smooth world coords
		if(newFinger)
		{
			j->worldCoords = world;
		} else
		{
			j->worldCoords.x = j->worldCoords.x * g_fingerScreenSmoothing + (1 - g_fingerScreenSmoothing) * world.x;
			j->worldCoords.y = j->worldCoords.y * g_fingerScreenSmoothing + (1 - g_fingerScreenSmoothing) * world.y;
			j->worldCoords.z = j->worldCoords.z * g_fingerScreenSmoothing + (1 - g_fingerScreenSmoothing) * world.z;
		}

		// update phone coords
		j->phoneCoords = m_phoneCalibration.ToPhoneSpace(j->worldCoords);
	}
}

void PMDCamera::UpdateFingers()
{
	Threshold(PMD_MAX_PHONE_DISTANCE);
	UpdateBackgroundSubtraction();
	MedianFilter();
	RemoveReflection();

	PMDUtils::DistancesToImage((float*)m_pmdDistancesProcessed->imageData, m_pmdDistancesProcessedRGB);

	FindConnectedComponentsInDistanceImage();
	FindBlobsInDistanceImage();
	
	// copy all new fingers to old fingers
	m_oldFingers.clear();
	for(vector<Finger>::iterator i = m_newFingers.begin(); i !=m_newFingers.end(); i++)
	{
		m_oldFingers.push_back(*i);
	}

	m_newFingers.clear();
	
	// associate fingers to blobs. new_fingers will be populated, blob point will be associated blob point, 
	// screen coords are still the old screen coords
	BlobsToFingers();


	UpdateFingerIdMask();

	UpdateFingerPositions();
}

// Applies a median filter to the distances image
// overrides the distances image
void PMDCamera::MedianFilter()
{
	IplImage* tmp = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), IPL_DEPTH_32F, 1);
	Mat src = m_pmdDistancesProcessed;
	Mat dst = tmp;
	// to do: no magic numbers!
	medianBlur(src, dst, 5);
	memcpy_s(m_pmdDistancesProcessed->imageData, PMDIMAGESIZE * sizeof(float), tmp->imageData, PMDIMAGESIZE * sizeof(float));
	cvReleaseImage(&tmp);
}

void PMDCamera::Erode(int erosionSize)
{
	IplImage* tmp = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), IPL_DEPTH_32F, 1);
	Mat src = m_pmdDistancesProcessed;
	Mat dst = tmp;
	Mat kernel = getStructuringElement(MORPH_ELLIPSE, Size(2*erosionSize + 1, 2 * erosionSize + 1), Point(erosionSize, erosionSize));
	erode(src, dst, kernel);
	memcpy_s(m_pmdDistancesProcessed->imageData, PMDIMAGESIZE * sizeof(float), tmp->imageData, PMDIMAGESIZE * sizeof(float));
	cvReleaseImage(&tmp);
}

void PMDCamera::RemoveReflection()
{
	float* pPhone = (float*) m_pmdPhoneSpace->imageData;
	float* pDistances = (float *)m_pmdDistancesProcessed->imageData;
	for(int i = 0; i < PMDIMAGESIZE; i++, pPhone += 3, ++pDistances)
	{
		if(pPhone[1] < 0 )
		{
			*pDistances = PMD_INVALID_DISTANCE;

			// try removing anything outside phone as well

		}
	}
}

void PMDCamera::RemoveOutsidePhone()
{
	float* pPhone = (float*) m_pmdPhoneSpace->imageData;
	float* pDistances = (float *)m_pmdDistancesProcessed->imageData;
	for(int i = 0; i < PMDIMAGESIZE; i++, pPhone += 3, ++pDistances)
	{
		if(pPhone[0] < 0 || pPhone[0] > 0.06f)
		{
			*pDistances = PMD_INVALID_DISTANCE;

			// try removing anything outside phone as well

		}
	}
}


void PMDCamera::FindBlobsInIntensityImage()
{
	if(m_blobIntensitiesFound) return;
	vector<KeyPoint> tmp;
	PMDUtils::AmplitudesToImage(m_pmdIntensitiesBuffer, m_pmdIntensitiesRGB);
	m_intensitiesBlobDetector->detect(m_pmdIntensitiesRGB, tmp);
	
	m_blobPointsIntensity.clear();
	for(vector<KeyPoint>::iterator i = tmp.begin(); i < tmp.end(); i++)
	{
		BlobPoint toAdd;
		toAdd.pt = i->pt;
		toAdd.size = i->size;
		m_blobPointsIntensity.push_back(toAdd);
	}

	m_blobIntensitiesFound = true;

}





