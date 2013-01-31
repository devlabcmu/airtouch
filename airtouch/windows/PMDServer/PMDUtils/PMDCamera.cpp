#include "PMDCamera.h"
#include <algorithm>
#define SOURCE_PLUGIN "camboardnano"
#define SOURCE_PARAM ""
#define PROC_PLUGIN "camboardnanoproc"
#define PROC_PARAM ""

#define FILE_SOURCE_PLUGIN "pmdfile"

const int g_numFramesForBackgroundSubtraction = 50;

const float g_fingerSmoothing = 0.2f;
const float g_fingerWorldSmoothing = 0.4f;

PMDCamera::PMDCamera(void)
{
	// create all opencv images
	m_pmdDistancesProcessed = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), IPL_DEPTH_32F, 1);
	m_pmdPhoneSpace = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), IPL_DEPTH_32F, 3);
	m_pmdCoords = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), IPL_DEPTH_32F, 3);
	m_pmdDistancesProcessedRGB = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), 8, 3); 
	m_pmdIntensitiesRGB = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), 8, 3);
	m_pmdFingerMaskRGB = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), 8, 3);

	SimpleBlobDetector::Params blobParams;
	blobParams.minThreshold = 1;
	blobParams.maxThreshold = 255;
	blobParams.thresholdStep = 100;
	
	blobParams.minDistBetweenBlobs = 1.0f;

	blobParams.filterByArea = true;
	blobParams.minArea = 300.0f;
	blobParams.maxArea = 100000.0f;

	blobParams.filterByCircularity = false;
	
	blobParams.filterByColor = true;
	blobParams.blobColor = 255;

	blobParams.filterByConvexity = false;
	blobParams.filterByInertia = false;
	

	blobParams.blobColor = 255;
	m_blobDetector = new SimpleBlobDetector(blobParams);
	m_blobDetector->create("DistancesBlob");

	blobParams.minThreshold = 100;
	blobParams.maxThreshold = 255;
	blobParams.thresholdStep = 50;
	
	blobParams.minArea = 1.0f;
	blobParams.maxArea = 100000.0f;
	m_intensitiesBlobDetector = new SimpleBlobDetector(blobParams);
	m_intensitiesBlobDetector->create("IntensitiesBlob");
}


PMDCamera::~PMDCamera(void)
{
	// release all opencv images
	cvReleaseImage(&m_pmdDistancesProcessed);
	cvReleaseImage(&m_pmdCoords);
	cvReleaseImage(&m_pmdPhoneSpace);
	cvReleaseImage(&m_pmdDistancesProcessedRGB);
	cvReleaseImage(&m_pmdIntensitiesRGB);
	cvReleaseImage(&m_pmdFingerMaskRGB);
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

HRESULT PMDCamera::InitializeCameraFromFile(char* filename)
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
		float* frame = new float[PMDIMAGESIZE];
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
		delete frames[i];
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

// call after blobstofingers but before findfingerpositions
void PMDCamera::UpdateFingerIdMask()
{
	vector<Point2i> pointsToExplore;
	memset(m_fingerIdMask, -2, PMDIMAGESIZE * sizeof(int));
	
	float* pDistancesProcessed = (float*)m_pmdDistancesProcessed->imageData;
	// -2 means unexplored
	// -1 means invalid
	// > 0 means the finger id
	CvScalar fingerColors[2] = {CV_RGB(255,0,0), CV_RGB(0,0,255)};
	cvSet(m_pmdFingerMaskRGB, CV_RGB(0,0,0));
	for (vector<Finger>::iterator i = m_newFingers.begin(); i < m_newFingers.end(); i++)
	{
		pointsToExplore.clear();
		pointsToExplore.push_back(Point2i(i->blobCenter));
		int id = i->id;
		while(!pointsToExplore.empty())
		{
			Point2i toExplore = pointsToExplore.back();
			pointsToExplore.pop_back();
			int idx = toExplore.y * PMDNUMCOLS + toExplore.x;
			
			// if the point has been explored already, continue
			if(m_fingerIdMask[idx] > -2) continue;
			// if the point is an invalid distance, mark the point in mask as invalid and continue
			if(pDistancesProcessed[idx] == PMD_INVALID_DISTANCE)
			{
				m_fingerIdMask[idx] = -1;
				continue;
			}

			// otherwise the point is a member of the blob, set mask value
			m_fingerIdMask[idx] = id;
			cvSet2D(m_pmdFingerMaskRGB, toExplore.y, toExplore.x, fingerColors[id % 2]);
			// also set color value

			// add all of its neighbors that are valid coordinates
			for(int dy = -1; dy <=1; dy++)
			{ 
				for(int dx = -1; dx <=1; dx++)
				{
					if(dx == 0 && dy == 0) continue;
					int x = toExplore.x + dx;
					int y = toExplore.y + dy;
					if(x < 0 || x >= PMDNUMCOLS || y < 0 || y >= PMDNUMROWS) continue;
					pointsToExplore.push_back(Point2i(x,y));
				}
			}
		}
	}
	
}

void PMDCamera::BlobsToFingers()
{
	// reduce number of blobs to max_fingers
	vector<KeyPoint> blobCopy;
	for (vector<KeyPoint>::iterator i = m_blobPoints.begin(); i < m_blobPoints.end(); i++)
	{
		blobCopy.push_back(*i);
	}

	if(blobCopy.size() > PMD_MAX_FINGERS)
	{
		std::sort(blobCopy.begin(), blobCopy.end(), PMDCamera::blobCompare);
		blobCopy.erase(blobCopy.begin() + PMD_MAX_FINGERS - 1, blobCopy.end());
	}

	if(blobCopy.size() == 0) return;

	for(vector<KeyPoint>::iterator i = blobCopy.begin(); i !=blobCopy.end(); i++)
	{
		Finger newFinger;

		// find the finger that is closest to the current blob position
		int closestIndex=0;
		float closestDistance = 10000.0f;
		Point2f blobCenter = i->pt;
		for(vector<Finger>::iterator j = m_oldFingers.begin(); j !=m_oldFingers.end(); j++)
		{
			float dist = norm(blobCenter - j->blobCenter);
			if(dist < closestDistance)
			{
				closestDistance = dist;
				closestIndex = j - m_oldFingers.begin();
			}
		}

		bool fingerBlobMatch = m_oldFingers.size() > 0;
		// check if there are any other blobs that are closer to this finger
		for(vector<KeyPoint>::iterator k = blobCopy.begin(); k !=blobCopy.end() && fingerBlobMatch; k++)
		{
			if(k == i) continue;
			float dist = norm(k->pt - m_oldFingers[closestIndex].blobCenter);
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

		newFinger.blobCenter = i->pt;
		newFinger.blobSize = i->size;
		m_newFingers.push_back(newFinger);
	}

}

Point2f PMDCamera::FindFingerPos(vector<Finger>::iterator f)
{
	float* pDistances = (float*) m_pmdDistancesProcessed->imageData;
	int x, y;
	float minZ = 1000.0f;
	float minX = 0, minY = 0;
	bool newFinger = f->screenCoords.x < 0;
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

Point2f PMDCamera::FindFingerPosUsingTracker(vector<Finger>::iterator f)
{
	Point2f result;
	return result;
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
		Point2f fingerPos = FindFingerPos(j);

		// smooth finger position screen space
		if(newFinger)
		{
			// if screen coords are new, just use those
			j->screenCoords = fingerPos;
		} else
		{
			// otherwise, smooth
			j->screenCoords = j->screenCoords * g_fingerSmoothing + fingerPos * (1 - g_fingerSmoothing);
		}

		// update world coords
		int x = j->screenCoords.x;
		int y = j->screenCoords.y;
		float* pWorld = (float*)m_pmdCoords->imageData;
		pWorld += 3 * (PMDNUMCOLS * y + x);
		Point3f world = Point3f(pWorld[0], pWorld[1], pWorld[2]);

		// smooth world coords
		if(newFinger)
		{
			j->worldCoords = world;
		} else
		{
			// todo: need to do something better if the coordinates are invalid
			if(pDistances[(PMDNUMCOLS * y + x)] != PMD_INVALID_DISTANCE)
			{
				j->worldCoords = j->worldCoords * g_fingerSmoothing + (1 - g_fingerWorldSmoothing) * world;
			}
			
		}

		// update phone coords
		j->phoneCoords = m_phoneCalibration.ToPhoneSpace(j->worldCoords);
	}
}

void PMDCamera::UpdateFingers()
{
	FindBlobs();
	
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

void PMDCamera::FindBlobs()
{
	PMDUtils::DistancesToImage((const float *)m_pmdDistancesProcessed->imageData, m_pmdDistancesProcessedRGB);
	m_blobDetector->detect(m_pmdDistancesProcessedRGB, m_blobPoints);
}


void PMDCamera::FindBlobsInIntensityImage()
{
	PMDUtils::AmplitudesToImage(m_pmdIntensitiesBuffer, m_pmdIntensitiesRGB);
	m_intensitiesBlobDetector->detect(m_pmdIntensitiesRGB, m_blobPointsIntensity);
}



