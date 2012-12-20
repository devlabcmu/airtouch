#pragma once 

#include <pmdsdk2.h>
#include "pmddata.h"


#define SOURCE_PLUGIN "camboardnano"
#define SOURCE_PARAM ""
#define PROC_PLUGIN "camboardnanoproc"
#define PROC_PARAM ""

// for file io
#define FILE_SOURCE_PLUGIN "pmdfile"

HRESULT initializePMD(PMDHandle* handle, char* err, int errlen)
{
	int res;

	res = pmdOpen (handle, SOURCE_PLUGIN, SOURCE_PARAM, PROC_PLUGIN, PROC_PARAM);
	if (res != PMD_OK)
	{
		pmdGetLastError (0, err, errlen);
		cout << "Could not connect: " << err << endl;
		return -1;
	}

	cout << "opened sensor" << endl;  

	return 0;
}

HRESULT initializePMDFromFile(PMDHandle* handle, char* filename, char* err, int errlen)
{
	int res;

	res = pmdOpen (handle, FILE_SOURCE_PLUGIN, filename , PROC_PLUGIN, PROC_PARAM);
	if (res != PMD_OK)
	{
		pmdGetLastError (0, err, errlen);
		cout << "Could not open file: " << err << endl;
		return -1;
	}

	cout << "opened sensor from file " << filename << endl;  

	return 0;
}