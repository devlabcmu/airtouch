//
//  xacData.h
//  ArBoIn
//
//  Created by Xiang 'Anthony' Chen on 1/11/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "xacVector.h"
#import "xacBuffer.h"

@interface xacData : NSObject

@property xacVector* vecRaw;
@property xacVector* vecFilt;
@property xacVector* maxRaw;
@property xacVector* minRaw;
//@property xacVector* vecCali;
@property BOOL isValid;

@property xacBuffer* bufRaw;

- (id) init: (int) bufSize;
- (void) update: (float)x :(float)y :(float)z;
- (void) gaussian: (int) n;
- (xacVector*) getCaliRaw;
- (xacVector*) getCaliFilt;
- (void) offsetRange: (float) val;
- (void) decay: (float)rate;
//- (xacVector*) getMappedCaliRaw;


- (xacVector*) getMean;
- (xacVector*) getStd;

@end
