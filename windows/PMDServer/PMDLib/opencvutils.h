#include <opencv/cxcore.h>

namespace cv{
void bandpass(InputArray _src, OutputArray _dst, uchar min, uchar max, uchar val)
{
	Mat src = _src.getMat();
	_dst.create(src.size(), src.type());
	Mat dst = _dst.getMat();

	for(int row = 0; row < src.rows; ++row) {
		uchar* pSrc = src.ptr(row);
		uchar* pDst = dst.ptr(row);
		for(int col = 0; col < src.cols; ++col, pSrc++, pDst++) {
			if(*pSrc < min  || *pSrc > max)
				*pDst = 0;
			else
				*pDst = val;
		}
	}
}
}