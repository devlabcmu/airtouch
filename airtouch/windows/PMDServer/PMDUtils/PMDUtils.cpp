#include "PMDUtils.h"

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