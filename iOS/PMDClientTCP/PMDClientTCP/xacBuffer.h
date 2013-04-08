//
//  xacBuffer.h
//  ArBoIn
//
//  Created by Xiang 'Anthony' Chen on 1/11/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "xacVector.h"

@interface xacBuffer : NSObject

@property NSMutableArray* buf;
@property int size;
@property xacVector* dymSum;

- (id) init: (int) size;
- (void) add: (xacVector*) obj;
- (xacVector*) avrgFilter: (int) lb;
- (xacVector*) gausFilter: (int) lb;

- (xacVector*) getMean;
- (xacVector*) getStd;

@end
