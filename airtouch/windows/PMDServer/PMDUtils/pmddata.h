#pragma once

#define PMDIMAGESIZE 19800
#define PMDREQUESTSIZE 512
#define PMDNUMCOLS 165
#define PMDNUMROWS 120

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
