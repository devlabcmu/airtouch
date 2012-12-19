#pragma once 

#include <pmdsdk2.h>
#include "pmddata.h"


#define SOURCE_PLUGIN "camboardnano"
#define SOURCE_PARAM ""
#define PROC_PLUGIN "camboardnanoproc"
#define PROC_PARAM ""

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