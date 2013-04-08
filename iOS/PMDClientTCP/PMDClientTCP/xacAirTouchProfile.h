//
//  xacAirTouchProfile.h
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 1/6/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface xacAirTouchProfile : NSObject

@property float X_MAX;
@property float Y_MAX;
@property float Z_MAX;

@property float rawX;
@property float rawY;
@property float rawZ;

@property float rawXMax;
@property float rawXMin;

@property float rawYMax;
@property float rawYMin;

@property float rawZMax;
@property float rawZMin;

@property float caliX;
@property float caliY;
@property float caliZ;

- (void) updateRawData :(float)x :(float)y :(float)z;

@end
