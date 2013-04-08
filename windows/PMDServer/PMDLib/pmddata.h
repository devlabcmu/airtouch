#pragma once

#include "PMDConstants.h"

typedef struct {
	int id; // 4 bytes
	float x; // 4 bytes
	float y; // 4 bytes
	float z; // 4 bytes
} PMDFinger;

typedef struct {
	PMDFinger fingers[2];
} PMDFingerData;



typedef struct {
	PMDFinger fingers[2];
	float buffer[PMDIMAGESIZE];
} PMDData;


typedef struct {
	char buffer[PMDREQUESTSIZE];
} PMDRequest;

