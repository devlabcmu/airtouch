
#include <opencv/cxcore.h>

namespace cv{

//! computes the connected components labeled image of boolean image I with 4 or 8 way connectivity - returns N, the total
//number of labels [0, N-1] where 0 represents the background label.
int connectedComponents(Mat &L, const Mat &I, int connectivity = 8);
}


