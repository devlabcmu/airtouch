#pragma once

#include "PMDConstants.h"

typedef struct {
	float fingerX;
	float fingerY;
	float fingerZ;
	float buffer[PMDIMAGESIZE];
} PMDData;

typedef struct {
	float fingerX;
	float fingerY;
	float fingerZ;
} PMDFingerData;

typedef struct {
	char buffer[PMDREQUESTSIZE];
} PMDRequest;

