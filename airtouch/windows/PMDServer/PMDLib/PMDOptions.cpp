#include "PMDOptions.h"


PMDOptions::PMDOptions(void)
{
	FileName = "";
	TrackingMode = FINGER_TRACKING_INTERPOLATE_BRIGHTEST;
}


PMDOptions::~PMDOptions(void)
{
}

void PMDOptions::PrintHelp()
{
	fprintf(stdout, "PMD arguments: [--use-ir-tracker|--interpolate-intensity|--interpolate-distance] [filename.pmd]\n\tfirst argument specifies finger tracking algorithm to use. \n\tDefault is interpolate based on intensity. \n\n\tsecond argument specifies file to use. \n\tIf not present uses camera stream.\n"); 
}

PMDOptions PMDOptions::ParseArgs(int argc, char** argv)
{
	PMDOptions result;
	result.backgroundSubtract = false;
	int argi = 1;
	bool fromCamera = true;
	while(argc > argi)
	{
		if(strcmp(argv[argi], "--use-ir-tracker") == 0)
		{
			result.TrackingMode = FINGER_TRACKING_BRIGHTEST;
		} else if (strcmp(argv[argi], "--interpolate-intensity") == 0)
		{
			result.TrackingMode = FINGER_TRACKING_INTERPOLATE_BRIGHTEST;
		} else if(strcmp(argv[argi], "--interpolate-distance") == 0)
		{
			result.TrackingMode = FINGER_TRACKING_INTERPOLATE_CLOSEST;
		} else if(strcmp(argv[argi], "--contours") == 0)
		{
			result.TrackingMode = FINGER_TRACKING_CONTOURS;
		}
		else if(strcmp(argv[argi], "--background-subtract") == 0)
		{
			result.backgroundSubtract = true;
		}
		else  if(argi == argc - 1)
		{
			result.FileName = argv[argi];
		} else 
		{
			cout << "Unknown argument: " << argv[argi] << endl;
		}
		argi++;
	}
	return result;
}
