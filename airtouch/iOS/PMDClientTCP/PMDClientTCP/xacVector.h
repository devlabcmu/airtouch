//
//  xacVector.h
//  ArBoIn
//
//  Created by Xiang 'Anthony' Chen on 1/11/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface xacVector : NSObject

//@property float x;
//@property float y;
//@property float z;

@property float X_MAX;
@property float Y_MAX;
@property float Z_MAX;

@property float rawX;
@property float rawY;
@property float rawZ;

//@property float rawXMax;
//@property float rawXMin;
//
//@property float rawYMax;
//@property float rawYMin;
//
//@property float rawZMax;
//@property float rawZMin;
//
//@property float caliX;
//@property float caliY;
//@property float caliZ;

- (id) init;
- (id) init :(float)x :(float)y :(float)z;
- (void) update :(float)x :(float)y :(float)z;
- (void) updateMax :(float)x :(float)y :(float)z;
- (void) updateMin :(float)x :(float)y :(float)z;
- (xacVector*) plus :(xacVector*) v1 :(xacVector*)v2;
- (void) multiply: (float)a;
- (xacVector*) getCopy;

@end
