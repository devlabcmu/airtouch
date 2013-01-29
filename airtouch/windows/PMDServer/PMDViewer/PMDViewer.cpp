// PMDViewer.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"

PMDCamera _pmdCamera;

// FPS
time_t _fpsStart, _fpsEnd;
double _fps = 0;
int _fpsCounter = 0;

// UI
IplImage* _data;

void error(string msg)
{
	fprintf(stderr, "%s\n", msg);
	cout << "Press any key to quit..."<<endl;
	char c = getchar();
	exit(EXIT_FAILURE);
}

void welcomeMessage()
{
	// add welcome here
	cout << "PMDViewer shows PMD data and various stages of processing algorithm" << endl;
	cout << "Usage: PMDViewer.exe to read from camera" << endl;
	cout << "Usage: PMDViewer.exe [filename.pmd] to read from .pmd file" << endl;
}

void showBackgroundImage()
{
	// todo: convert to a method (repeated once already)
	BackgroundSubtractionData const* const backgroundData = _pmdCamera.GetBackgroundSubtractionData();
	IplImage* toShow = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), 8, 3);
	_pmdCamera.RenderDistances(backgroundData->means, toShow);
	
	cvFlip (toShow, toShow, 0);

    // Display the image
    cvShowImage ("Background Image", toShow);
	cvWaitKey(1);

	cvReleaseImage(&toShow);

	//// show standard deviation
	toShow = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), 8, 3);
	_pmdCamera.RenderDistances(backgroundData->stdevs, toShow);

	cvFlip (toShow, toShow, 0);
	cvShowImage ("Stdev Image", toShow);
	cvWaitKey(1);
	cvReleaseImage(&toShow);
}

bool update()
{
	HRESULT hr = _pmdCamera.UpdateCameraData();
	if(!SUCCEEDED(hr))
	{
		fprintf(stderr, "Failed to update camera data\n");
		return false;
	}
	_pmdCamera.Threshold(0.5f);
	_pmdCamera.UpdateBackgroundSubtraction();
	_pmdCamera.MedianFilter();
	_pmdCamera.UpdateFingers();
	return true;
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

	_pmdCamera.RenderDistances(_pmdCamera.GetDistancesProcessed(), _data);

    cvFlip (_data, _data, 0);
	cvCircle(_data, cvPoint(cvRound(_pmdCamera.GetFingerData()->fingerX), 
		PMDNUMROWS - cvRound(_pmdCamera.GetFingerData()->fingerY)), 
		10, 
		CV_RGB(255,0,0));

	ostringstream str;
	str.precision(2);
	str << "fps: " << _fps;
    
	Mat img = _data;
	putText(img, str.str(), cvPoint(10,20), FONT_HERSHEY_COMPLEX_SMALL, 1.0f,CV_RGB(255,0,0)) ;
	cvShowImage ("Data", _data);
	return true;
}

int  main(int argc, char* argv[])
{
	// welcome message
	welcomeMessage();
	
	_data = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), 8, 3);

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
	showBackgroundImage();
	
	while(true)
	{
		if(!update()) break;
		if(!draw()) break;
		int key = cvWaitKey(1);
		if(key == 'q') break;
	}

	cvReleaseImage(&_data);

	return 0;
}

