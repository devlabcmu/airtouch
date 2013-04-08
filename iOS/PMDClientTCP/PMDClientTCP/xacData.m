//
//  xacData.m
//  ArBoIn
//
//  Created by Xiang 'Anthony' Chen on 1/11/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacData.h"

@implementation xacData

float xThis = -1;
float yThis = -1;
float zThis = -1;

float xLast = -1;
float yLast = -1;
float zLast = -1;

- (id) init: (int)bufSize
{
    self = [super init];
    
    _vecRaw = [[xacVector alloc] init];
    _vecFilt = _vecRaw;// [[xacVector alloc] init];
    _maxRaw = [[xacVector alloc] init];
    _minRaw = [[xacVector alloc] init];
//    _vecCali = [[xacVector alloc] init];
    
    _bufRaw = [[xacBuffer alloc] init: bufSize];
    
    
    
    return self;
}

- (void) update: (float)x :(float)y :(float)z
{
    xThis = x;
    yThis = y;
    zThis = z;
    
    [self doDataCleaning];
    
    [_vecRaw update:xThis :yThis :zThis];
    [_maxRaw updateMax:xThis :yThis :zThis];
    [_minRaw updateMin:xThis :yThis :zThis];
//    [self updateCali];
    
//    [_bufRaw add:[_vecRaw getCopy]];
}

- (void) doDataCleaning
{
    int thrsJumpX = 30;
//    int thrsJumpY = 0.2;
    int thrsJumpZ = 50;
    
    xThis = fabs(xThis - xLast) > thrsJumpX ? xLast : xThis;
//    yThis = fabs(yThis - yLast) > thrsJumpY ? yLast : yThis;
    zThis = fabs(zThis - zLast) > thrsJumpZ ? zLast : zThis;
    
    xLast = xThis;
//    yLast = yThis;
    zLast = zThis;
}

- (void) decay: (float)rate
{
    xacVector* midRaw = [[xacVector alloc] init:(_maxRaw.rawX + _minRaw.rawX) / 2
                                               :(_maxRaw.rawY + _minRaw.rawY) / 2
                                               :(_maxRaw.rawZ + _minRaw.rawZ) / 2 ];
//    _maxRaw = [midRaw multiply:rate] [_maxRaw multiply:rate];
    
    _maxRaw.rawX = _maxRaw.rawX * (1 - rate) + midRaw.rawX * rate;
    _maxRaw.rawY = _maxRaw.rawY * (1 - rate) + midRaw.rawY * rate;
    _maxRaw.rawZ = _maxRaw.rawZ * (1 - rate) + midRaw.rawZ * rate;
    
    _minRaw.rawX = _minRaw.rawX * (1 - rate) + midRaw.rawX * rate;
    _minRaw.rawY = _minRaw.rawY * (1 - rate) + midRaw.rawY * rate;
    _minRaw.rawZ = _minRaw.rawZ * (1 - rate) + midRaw.rawZ * rate;
    
}

- (xacVector*) getCaliRaw
{
    float one = 0.001;
    float xCali = (_vecRaw.rawX - _minRaw.rawX + one) / (_maxRaw.rawX - _minRaw.rawX + one);
    float yCali = (_vecRaw.rawY - _minRaw.rawY + one) / (_maxRaw.rawY - _minRaw.rawY + one);
    float zCali = (_vecRaw.rawZ - _minRaw.rawZ + one) / (_maxRaw.rawZ - _minRaw.rawZ + one);
//    [_vecCali update:xCali :yCali :zCali];
    
    return [[xacVector alloc] init:xCali :yCali :zCali];
}

//- (xacVector*) getMappedCaliRaw
//{
//    float one = 0.001;
//    float xCali = (cos(_vecRaw.rawX * M_PI / 180.0) - cos(_minRaw.rawX * M_PI / 180.0) + one) / (cos(_maxRaw.rawX * M_PI / 180.0) - cos(_minRaw.rawX * M_PI / 180.0) + one);
//    float yCali = (cos(_vecRaw.rawY * M_PI / 180.0) - cos(_minRaw.rawY * M_PI / 180.0) + one) / (cos(_maxRaw.rawY * M_PI / 180.0) - cos(_minRaw.rawY * M_PI / 180.0) + one);
//    float zCali = (cos(_vecRaw.rawZ * M_PI / 180.0) - cos(_minRaw.rawZ * M_PI / 180.0) + one) / (cos(_maxRaw.rawZ * M_PI / 180.0) - cos(_minRaw.rawZ * M_PI / 180.0) + one);
//    
//    if(xCali > 1 || xCali < -1)
//    {
//        int brk = 0;
//        brk++;
//    }
//    
//    return [[xacVector alloc] init:xCali :yCali :zCali];
//}

- (xacVector*) getCaliFilt;
{
    if(_vecFilt == nil)
    {
        return nil;
    }
    
    float one = 0.001;
    float xCali = (_vecFilt.rawX - _minRaw.rawX + one) / (_maxRaw.rawX - _minRaw.rawX + one);
    float yCali = (_vecFilt.rawY - _minRaw.rawY + one) / (_maxRaw.rawY - _minRaw.rawY + one);
    float zCali = (_vecFilt.rawZ - _minRaw.rawZ + one) / (_maxRaw.rawZ - _minRaw.rawZ + one);
    //    [_vecCali update:xCali :yCali :zCali];
    
    return [[xacVector alloc] init:xCali :yCali :zCali];
}


- (void) gaussian: (int) n
{
    _vecFilt = [_bufRaw gausFilter:n];
    
    if(_vecFilt != nil)
    {
        int k = 0;
        k = k+1;
    }
}

- (void) offsetRange: (float) val
{
    _maxRaw.rawX += val;
    _maxRaw.rawY += val;
    _maxRaw.rawZ += val;
    
    _minRaw.rawX += val;
    _minRaw.rawY += val;
    _minRaw.rawZ += val;
}


- (xacVector*) getMean
{
//    _bufRaw
}

- (xacVector*) getStd
{
    
}

@end
