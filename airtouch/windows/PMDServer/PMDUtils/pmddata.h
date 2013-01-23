#pragma once

#define PMDIMAGESIZE 19800
#define PMDREQUESTSIZE 512
#define PMDNUMCOLS 165
#define PMDNUMROWS 120

typedef struct {
	float fingerX;
	float fingerY;
	float fingerZ;
	float buffer[PMDIMAGESIZE];
} PMDData;

typedef struct {
	float fingerX;
	float fingerY;
	float fingerZ;
} PMDFingerData;

typedef struct {
	char buffer[PMDREQUESTSIZE];
} PMDRequest;


void depthDataToImage( float const* pDepthData, unsigned char* imgPtr, int rowStep, int step)
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
