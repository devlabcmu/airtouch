//
//  xacAirTouchProfile.m
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 1/6/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacAirTouchProfile.h"

@implementation xacAirTouchProfile

- (id) init
{
    self = [super init];
    
    _X_MAX = 1.0e3;
    _Y_MAX = 1.0e3;
    _Z_MAX = 1.0e1;

    return self;
}

- (void) updateRawData: (float)x: (float)y: (float)z
{
    float one = 0.001;
    
    _rawX = x > _X_MAX ? _X_MAX : x;
    _rawXMax = _rawX > _rawXMax ? _rawX : _rawXMax;
    _rawXMin = _rawX < _rawXMin ? _rawX : _rawXMin;
    _caliX = (_rawX - _rawXMin + one) / (_rawXMax - _rawXMin + one);
    
    _rawY = y > _Y_MAX ? _Y_MAX : y;
    _rawYMax = _rawY > _rawYMax ? _rawY : _rawYMax;
    _rawYMin = _rawY < _rawYMin ? _rawY : _rawYMin;
    _caliY = (_rawY - _rawYMin + one) / (_rawYMax - _rawYMin + one);
    
    _rawZ = z > _Z_MAX ? _Z_MAX : z;
    _rawZMax = _rawZ > _rawZMax ? _rawZ : _rawZMax;
    _rawZMin = _rawZ < _rawZMin ? _rawZ : _rawZMin;
    _caliZ = (_rawZ - _rawZMin + one) / (_rawZMax - _rawZMin + one);
}

@end
