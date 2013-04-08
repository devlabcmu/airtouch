#pragma once
#include <string>
#include <iostream>
#include "PMDCamera.h"

using namespace std;



class PMDOptions
{
public:
	PMDFingerTrackingMode TrackingMode;
	string FileName;
	bool usbCommunicationForAndroid;
	bool backgroundSubtract;
	static PMDOptions ParseArgs(int argc, char** argv);
	static void PrintHelp();

	PMDOptions(void);
	~PMDOptions(void);


};

