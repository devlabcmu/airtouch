#include "PMDUtils.h"
#include <math.h>


void PMDUtils::DistancesToImage( float const* pDepthData, unsigned char* imgPtr, int rowStep, int step)
{
	unsigned char * currentRow = 0;
	for (int y = 0; y < PMDNUMROWS; ++y)
    {
		currentRow = &imgPtr[y * rowStep];
		for (int x = 0; x < PMDNUMCOLS; ++x, currentRow += step, ++pDepthData)
        {
			unsigned char val;
            // Clamp at 1 meters and scale the values in between to fit the image
			if (*pDepthData >= PMD_MAX_DISTANCE || *pDepthData == PMD_INVALID_DISTANCE)
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

void PMDUtils::AmplitudesToImage( float const* pAmplitudes, unsigned char* imgPtr, int rowStep, int step)
{
	float maxAmp = 0.0f;
	for(int i = 0; i < PMDIMAGESIZE; i++)
	{
		if(pAmplitudes[i] > maxAmp) maxAmp = pAmplitudes[i];
	}
	unsigned char * currentRow = 0;
	for (int y = 0; y < PMDNUMROWS; ++y)
    {
		currentRow = &imgPtr[y * rowStep];
		for (int x = 0; x < PMDNUMCOLS; ++x, currentRow += step, ++pAmplitudes)
        {
			unsigned char val = (char)(*pAmplitudes / maxAmp * 255.0f);
            // Clamp at 1 meters and scale the values in between to fit the image
			currentRow[0] = val;
			currentRow[1] = val;
			currentRow[2] = val;
        }
    }
}

void PMDUtils::CoordsToImage(float const* pCoords, UINT const* pFlags, unsigned char* imgPtr, int rowStep, int step)
{
	unsigned char* currentRow = 0;
	float minVal = -0.2f;
	float maxVal = 0.2f;
	float minZ = 0.0f;
	float maxZ = 0.5f;

	for (int y = 0; y < PMDNUMROWS; ++y)
    {
		currentRow = &imgPtr[y * rowStep];
		for (int x = 0; x < PMDNUMCOLS; ++x, currentRow += step, pCoords += 3, ++pFlags)
        {
			if(*pFlags & PMD_FLAG_INVALID)
			{
				memset(currentRow, 0, 3);
				continue;
			}
			unsigned char vx = (char)((pCoords[0] - minVal) / (maxVal - minVal) * 255.0f);
			unsigned char vy = (char)((pCoords[1] - minVal) / (maxVal - minVal) * 255.0f);
			unsigned char vz = (char)((pCoords[2] - minZ) / (maxZ - minZ) * 255.0f);
            // Clamp at 1 meters and scale the values in between to fit the image
			currentRow[0] = vx;
			currentRow[1] = vy;
			currentRow[2] = vz;
        }
    }
}