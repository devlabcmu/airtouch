// PMDViewer.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"

PMDCamera _pmdCamera;

// FPS
time_t _fpsStart, _fpsEnd;
double _fps = 0;
int _fpsCounter = 0;

// UI
vector<IplImage*> _images;
string _frameTitles[5] = {"intensities", "distances", "before reflection removal", "finger mask", "final"};

Mat _phoneSpace;
PhoneCalibration _phoneCalibration;

float _xBuffer[PMDIMAGESIZE];
float _yBuffer[PMDIMAGESIZE];
float _zBuffer[PMDIMAGESIZE];

void mouse_callback(int event, int x, int y, int flags, void* param)
{
	if(event == CV_EVENT_LBUTTONDOWN){
		IplImage* image = (IplImage*) param;
		Point3f p = _pmdCamera.GetCoord(image->height -y, image->width - x);
		Point3f phone = _phoneCalibration.ToPhoneSpace(p);
		fprintf(stdout, "mouse: (%d, %d), world:(%.4f,%.4f,%.4f), phone: (%.4f,%.4f,%.4f) \n", x, y, p.x, p.y, p.z, phone.x, phone.y, phone.z);
	}
}

void error(string msg)
{
	fprintf(stderr, "%s\n", msg);
	cout << "Press any key to quit..."<<endl;
	char c = getchar();
	exit(EXIT_FAILURE);
}

void welcomeMessage()
{
	cout << "PMDViewer shows PMD data and various stages of processing algorithm" << endl;
	cout << "Usage: PMDViewer.exe to read from camera" << endl;
	cout << "Usage: PMDViewer.exe [filename.pmd] to read from .pmd file" << endl;
}

void showBackgroundImage()
{
	// todo: convert to a method (repeated once already)
	BackgroundSubtractionData const* const backgroundData = _pmdCamera.GetBackgroundSubtractionData();
	IplImage* toShow = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), 8, 3);
	PMDUtils::DistancesToImage(backgroundData->means, toShow);
	
	cvFlip (toShow, toShow, 0);

    // Display the image
    cvShowImage ("Background Image", toShow);
	cvWaitKey(1);

	cvReleaseImage(&toShow);

	//// show standard deviation
	toShow = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), 8, 3);
	PMDUtils::DistancesToImage(backgroundData->stdevs, toShow);

	cvFlip (toShow, toShow, 0);
	cvShowImage ("Stdev Image", toShow);
	cvWaitKey(1);
	cvReleaseImage(&toShow);
}

bool fingerCompare(Finger a, Finger b){ return a.id < b.id;}

bool update()
{
	HRESULT hr = _pmdCamera.UpdateCameraData();
	if(!SUCCEEDED(hr))
	{
		fprintf(stderr, "Failed to update camera data\n");
		return false;
	}
	int imageIndex = 0;

	// update amplitudes
	PMDUtils::AmplitudesToImage(_pmdCamera.GetIntensitiesBuffer(), (unsigned char*) _images[imageIndex]->imageData,  
		_images[imageIndex]->widthStep / sizeof(unsigned char), _images[imageIndex]->nChannels);

	
	_pmdCamera.FindBlobsInIntensityImage();
	vector<KeyPoint> blobs = _pmdCamera.GetBlobPointsIntensities();
	for(vector<KeyPoint>::iterator i = blobs.begin(); i < blobs.end(); i++)
	{
		cvCircle(_images[imageIndex], i->pt, i->size, CV_RGB(0, 255, 0), 5);
	}

	imageIndex++;
	
	// draw distances
	PMDUtils::DistancesToImage(_pmdCamera.GetDistanceBuffer(), _images[imageIndex]);
	imageIndex++;

	_pmdCamera.Threshold(PMD_MAX_PHONE_DISTANCE);
	_pmdCamera.UpdateBackgroundSubtraction();
	_pmdCamera.MedianFilter();

	// update processed distances BEFORE reflection removal
	PMDUtils::DistancesToImage((const float *)_pmdCamera.GetDistancesProcessed()->imageData, _images[imageIndex]);
	imageIndex++;

	_pmdCamera.RemoveReflection();
	_pmdCamera.Erode(1);
	_pmdCamera.UpdateFingers();
	vector<Finger> fingers = _pmdCamera.GetFingers();
	CvScalar fingerColors[2] = {CV_RGB(255,0,0), CV_RGB(0,0,255)};
	
	
	// draw finger mask
	cvSet(_images[imageIndex], CV_RGB(0,0,0));
	const char* pFingerIdMask = _pmdCamera.GetFingerIdMask();
	for(vector<Finger>::iterator i = fingers.begin(); i !=fingers.end(); i++)
	{
		int id = i->id;
		int mask[PMDIMAGESIZE];
		ZeroMemory(mask, PMDIMAGESIZE * sizeof(int));
		for(int j = 0; j < PMDIMAGESIZE; j++)
		{
			if(pFingerIdMask[j] == id) 
			{
				cvSet1D(_images[imageIndex], j, fingerColors[id % 2]);
			}
		}
	}

	imageIndex++;

	// draw the final image
	memcpy(_images[imageIndex]->imageData, _pmdCamera.GetDistancesProcessedRGB()->imageData, _pmdCamera.GetDistancesProcessedRGB()->imageSize);

	blobs = _pmdCamera.GetBlobPoints();
	for(vector<KeyPoint>::iterator i = blobs.begin(); i < blobs.end(); i++)
	{
		cvCircle(_images[imageIndex], i->pt, i->size, CV_RGB(0, 255, 0));
	}


	Mat img = _images[imageIndex];

	for(vector<Finger>::iterator i = fingers.begin(); i !=fingers.end(); i++)
	{
		cvCircle(_images[imageIndex], i->screenCoords, 10, fingerColors[i->id % 2]);
	}
	return true;
}

void setup(int argc, char* argv[])
{
	// welcome message
	welcomeMessage();

	HRESULT hr = 0;

	// Initialize the PMD camera
	// check if we have parameters, if so first param is filename
	if(argc > 1)
	{
		char* filename = argv[1];
		cout << "Reading data from file " << filename << endl;
		hr = _pmdCamera.InitializeCameraFromFile(filename);
		if(!SUCCEEDED(hr)) error("Error: failed to initialize from file");
	} else 
	{
		hr = _pmdCamera.InitializeCamera();
		if(!SUCCEEDED(hr)) error("Error: failed to initialize PMD camera");
	}

	hr = _pmdCamera.InitializeBackgroundSubtraction();
	if(!SUCCEEDED(hr)) error("Error: Background subtraction failed");

	_phoneSpace = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), IPL_DEPTH_32F, 3);
}
void getFps()
{
	// calculate current fps
	time(&_fpsEnd);
	
	double sec = difftime(_fpsEnd, _fpsStart);
	if(sec > 1)
	{
		_fps = _fpsCounter;
		time(&_fpsStart);
		_fpsCounter = 0;
	} else
	{
		++_fpsCounter;
	}
}


bool draw()
{
	getFps();



	ostringstream str;
	str.precision(2);
	str << "fps: " << _fps;
    
	for(UINT i = 0; i < _images.size(); i++)
	{
		cvFlip (_images[i], _images[i], -1);
		Mat img = _images[i];
		putText(img, str.str(), cvPoint(10,20), FONT_HERSHEY_COMPLEX_SMALL, 1.0f,CV_RGB(255,0,0)) ;
		cvShowImage (_frameTitles[i].c_str(), _images[i]);
	}
    
	return true;
}

void createImages()
{
	for(int i = 0; i < _countof(_frameTitles);i++)
	{
		IplImage* toAdd = cvCreateImage(cvSize(PMDNUMCOLS, PMDNUMROWS), 8,3); 
		_images.push_back(toAdd);
		cvNamedWindow(_frameTitles[i].c_str());
		cvSetMouseCallback( _frameTitles[i].c_str(), mouse_callback, toAdd);
	}
}

void destroyImages()
{
	for(int i = 0; i < _countof(_frameTitles);i++)
	{
		cvReleaseImage(&_images[i]);
	}
	_images.clear();
}

int  main(int argc, char* argv[])
{
	setup(argc, argv);
	
	createImages();
	
	while(true)
	{
		if(!update()) break;
		if(!draw()) break;
		int key = cvWaitKey(1);
		if(key == 'q') break;
	}
	destroyImages();
	return 0;
}

