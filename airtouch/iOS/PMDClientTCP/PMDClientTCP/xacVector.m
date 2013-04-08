//
//  xacVector.m
//  ArBoIn
//
//  Created by Xiang 'Anthony' Chen on 1/11/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacVector.h"

@implementation xacVector

- (id) init
{
    self = [super init];
    
    _X_MAX = 1.0e3;
    _Y_MAX = 1.0e3;
    _Z_MAX = 1.0e1;
    
    _rawX = 0;
    _rawY = 0;
    _rawZ = 0;
    
    return self;
}

- (id) init:(float)x :(float)y :(float)z
{
    self = [super init];
    
    _X_MAX = 1.0e3;
    _Y_MAX = 1.0e3;
    _Z_MAX = 1.0e3;
    
    [self update:x :y :z];
    
    return self;
}

- (void) update :(float)x :(float)y :(float)z
{    
    if(fabs(x) < _X_MAX && fabs(y) < _Y_MAX && fabs(z) < _Z_MAX)
    {
        _rawX = x;// > _X_MAX ? _X_MAX : x;
        _rawY = y;// > _Y_MAX ? _Y_MAX : y;
        _rawZ = z;// > _Z_MAX ? _Z_MAX : z;
    }
}

- (void) updateMax :(float)x :(float)y :(float)z
{
    if(fabs(x) < _X_MAX && fabs(y) < _Y_MAX && fabs(z) < _Z_MAX)
    {
        _rawX = x > _rawX ? x : _rawX;
        _rawY = y > _rawY ? y : _rawY;
        _rawZ = z > _rawZ ? z : _rawZ;
    }
}

- (void) updateMin :(float)x :(float)y :(float)z
{
    if(fabs(x) < _X_MAX && fabs(y) < _Y_MAX && fabs(z) < _Z_MAX)
    {
        _rawX = x < _rawX ? x : _rawX;
        _rawY = y < _rawY ? y : _rawY;
        _rawZ = z < _rawZ ? z : _rawZ;
    }
}

- (void) multiply:(float)a
{
    [self update:_rawX * a :_rawY * a :_rawZ * a];
}

- (xacVector*) plus :(xacVector*) v1 :(xacVector*)v2
{
    return [[xacVector alloc] init:v1.rawX + v2.rawX :v1.rawY + v2.rawY :v1.rawZ + v2.rawZ];
}

- (xacVector*) getCopy
{
    xacVector* vCopy = [[xacVector alloc] init: _rawX: _rawY: _rawZ];
    return vCopy;
}

@end
