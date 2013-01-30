#pragma once

#include "PMDConstants.h"

typedef struct {
	float finger1X;
	float finger1Y;
	float finger1Z;

	float finger2X;
	float finger2Y;
	float finger2Z;
} PMDFingerData;

typedef struct {
	float finger1X;
	float finger1Y;
	float finger1Z;

	float finger2X;
	float finger2Y;
	float finger2Z;
	float buffer[PMDIMAGESIZE];
} PMDData;


typedef struct {
	char buffer[PMDREQUESTSIZE];
} PMDRequest;

