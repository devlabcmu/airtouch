#pragma once

#define PMDIMAGESIZE 19800
#define PMDBUFSIZE 512
#define PMDNUMCOLS 165
#define PMDNUMROWS 120

typedef struct {
	float fingerX;
	float fingerY;
	float fingerZ;
	float buffer[PMDIMAGESIZE];
} PMDSendData;

typedef struct {
	char buffer[PMDBUFSIZE];
} PMDReceiveData;
