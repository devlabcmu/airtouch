//
//  xacBuffer.m
//  ArBoIn
//
//  Created by Xiang 'Anthony' Chen on 1/11/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacBuffer.h"

@implementation xacBuffer

int ptrBuffer = -1;

- (id) init:(int)size
{
    self = [super init];

    _size = size;
    _buf = [NSMutableArray arrayWithCapacity:_size];
    ptrBuffer = 0;
    
    _dymSum = [[xacVector alloc] init];
    
    return self;
}

- (void) add:(xacVector*) obj
{
    if(_buf.count >= _size)
    {
        xacVector* tmpVector = (xacVector*)[_buf objectAtIndex:0];
        _dymSum.rawX -= tmpVector.rawX;
        _dymSum.rawY -= tmpVector.rawY;
        _dymSum.rawZ -= tmpVector.rawZ;
        
        [_buf removeObjectAtIndex:0];
    }
    
    [_buf addObject:obj];
    _dymSum.rawX += obj.rawX;
    _dymSum.rawY += obj.rawY;
    _dymSum.rawZ += obj.rawZ;
    
    ptrBuffer = (ptrBuffer + 1) % _size;
    
//    NSLog(@"%@", [NSString stringWithFormat:@"%d, %f", ptr, ((xacVector*)[_buf objectAtIndex:_buf.count - 1]).rawX]);
}

- (xacVector*) avrgFilter: (int) lb
{
    return nil;
}

- (xacVector*) gausFilter: (int) lb
{
    if(lb >= _buf.count)
    {
        return nil;
    }
    
    float sigma = 1;
    xacVector* s = [[xacVector alloc] init];
    float sumWeight = 0;
    
    for (int i = -lb; i <= lb; i++) {

        float w = (1 / (sqrt(2 * M_PI) * sigma)) * exp( - i * i / (2 * sigma));
        int idx = (int)(_buf.count - 1 - abs(i));
        xacVector* v = [_buf objectAtIndex:idx];
        s.rawX += v.rawX * w;
        s.rawY += v.rawY * w;
        s.rawZ += v.rawZ * w;
        sumWeight += w;
        
//        NSLog(@"%@", [NSString stringWithFormat:@"%d: %f", idx, w]);
    }
    
    [s multiply:(1 / sumWeight)];
    
//    return [[xacVector alloc] init:s.rawX :s.rawY :s.rawZ];
    return s;
}

- (xacVector*) getMean
{
//{
//    xacVector* sumVector = [[xacVector alloc] init:0 :0 :0];
//    for (int i = 0; i < _buf.count; i++) {
//        xacVector* tmpVector = (xacVector*)[_buf objectAtIndex:i];
//        sumVector.rawX += tmpVector.rawX;
//        sumVector.rawY += tmpVector.rawY;
//        sumVector.rawZ += tmpVector.rawZ;
//    }
//    
//    if(_buf.count > 0)
//    {
//        sumVector.rawX /= _buf.count;
//        sumVector.rawY /= _buf.count;
//        sumVector.rawZ /= _buf.count;
//    }
//    
//    return sumVector;


    xacVector* sumVector = [_dymSum getCopy];

    if(_buf.count > 0)
    {
        sumVector.rawX /= _buf.count;
        sumVector.rawY /= _buf.count;
        sumVector.rawZ /= _buf.count;
    }

    return sumVector;
}

- (xacVector*) getStd
{
    xacVector* stdVector = [[xacVector alloc] init:0 :0 :0];
    for (int i = 0; i < _buf.count; i++) {
        xacVector* tmpVector = (xacVector*)[_buf objectAtIndex:i];
        stdVector.rawX += (tmpVector.rawX - _dymSum.rawX / _buf.count) * (tmpVector.rawX - _dymSum.rawX / _buf.count);
        stdVector.rawY += (tmpVector.rawY - _dymSum.rawY / _buf.count) * (tmpVector.rawY - _dymSum.rawY / _buf.count);
        stdVector.rawZ += (tmpVector.rawZ - _dymSum.rawZ / _buf.count) * (tmpVector.rawZ - _dymSum.rawZ / _buf.count);
    }
    
    if(_buf.count > 0)
    {
        stdVector.rawX = sqrtf(stdVector.rawX / _buf.count);
        stdVector.rawY = sqrtf(stdVector.rawY / _buf.count);
        stdVector.rawZ = sqrtf(stdVector.rawZ / _buf.count);
    }
    
    return stdVector;
}

@end
