#include "PMDCamera.h"

#define SOURCE_PLUGIN "camboardnano"
#define SOURCE_PARAM ""
#define PROC_PLUGIN "camboardnanoproc"
#define PROC_PARAM ""
#define PMD_INVALID_DISTANCE -1
#define FILE_SOURCE_PLUGIN "pmdfile"

const int g_numFramesForBackgroundSubtraction = 50;


PMDCamera::PMDCamera(void)
{
	// create all opencv images
	m_pmdDistancesProcessed = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), IPL_DEPTH_32F, 1);
}


PMDCamera::~PMDCamera(void)
{
	// release all opencv images
	cvReleaseImage(&m_pmdDistancesProcessed);
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


	res = pmdGetFlags(m_pmdHandle, m_pmdFlags, PMDIMAGESIZE * 4);
	if (res != PMD_OK)
	{
		pmdGetLastError (m_pmdHandle, m_pmdErrorBuffer, 128);
		fprintf (stderr, "UpdateCameraData Error: Could not get flags: %s\n", m_pmdErrorBuffer);
		pmdClose (m_pmdHandle);
		return -1;
	}


	for(int y = 0; y < PMDNUMROWS; y++)
	{
		for(int x = 0; x < PMDNUMCOLS; x++)
		{
			int idx = y * PMDNUMCOLS + x;
			if(m_pmdFlags[idx] & PMD_FLAG_INVALID)
			{
				m_pmdDistanceBuffer[idx] = PMD_INVALID_DISTANCE;
			}
		}
	}

	// copy the raw data to the processed data
	memcpy_s(m_pmdDistancesProcessed->imageData, PMDIMAGESIZE * sizeof(float), m_pmdDistanceBuffer, PMDIMAGESIZE * sizeof(float));

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

void PMDCamera::RenderDistances(const float* src, IplImage* dst)
{
	DepthDataToImage(src, (unsigned char *) dst->imageData, dst->widthStep / sizeof(unsigned char), dst->nChannels);
}

void PMDCamera::RenderDistances(const IplImage* src, IplImage* dst)
{
	RenderDistances((const float *)src->imageData, dst);
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

void PMDCamera::UpdateFingers()
{
	// find the x, y coordinate that has the highest value
	m_pmdFingerData.fingerX = 0;
	m_pmdFingerData.fingerY = 0;
	m_pmdFingerData.fingerZ = FLT_MAX;

	for(int y = 0; y < PMDNUMROWS; y++)
	{
		for(int x = 0; x < PMDNUMCOLS; x++)
		{
			int idx = y * PMDNUMCOLS + x;
			if(m_pmdDistanceBuffer[idx] >= 0 && m_pmdDistanceBuffer[idx] < m_pmdFingerData.fingerZ)
			{
				m_pmdFingerData.fingerX = x;
				m_pmdFingerData.fingerY = y;
				m_pmdFingerData.fingerZ = m_pmdDistanceBuffer[idx];
			}
		}
	}

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

void PMDCamera::DepthDataToImage( float const* pDepthData, unsigned char* imgPtr, int rowStep, int step)
{
	unsigned char * currentRow = 0;
	for (int y = 0; y < PMDNUMROWS; ++y)
    {
		currentRow = &imgPtr[y * rowStep];
		for (int x = 0; x < PMDNUMCOLS; ++x, currentRow += step, ++pDepthData)
        {
			unsigned char val;
            // Clamp at 1 meters and scale the values in between to fit the image
            if (*pDepthData >= 1.0f || *pDepthData <= 0)
            {
                val = 0;
            }
            else
            {
                val = 255 - (unsigned char) (*pDepthData * 255.0f);
            }
			currentRow[0] = val;
			currentRow[1] = val;
			currentRow[2] = val;
        }
    }
}

void PMDCamera::AmplitudesToImage( float const* pDepthData, unsigned char* imgPtr, int rowStep, int step)
{
	unsigned char * currentRow = 0;
	for (int y = 0; y < PMDNUMROWS; ++y)
    {
		currentRow = &imgPtr[y * rowStep];
		for (int x = 0; x < PMDNUMCOLS; ++x, currentRow += step, ++pDepthData)
        {
			unsigned char val = *pDepthData;
            // Clamp at 1 meters and scale the values in between to fit the image
            if (*pDepthData > 255 || *pDepthData < 0)
            {
                val = 0;
            }
			currentRow[0] = val;
			currentRow[1] = val;
			currentRow[2] = val;
        }
    }
}



