//
//  xacTouchProfile.h
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 1/6/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "xacTouchPoint.h"

@interface xacScreenTouchProfile : NSObject

//@property int tid;
@property int count;
@property NSMutableArray* points;

- (id) init;
- (void) updateTouchPoints: (int)id: (float)x: (float)y;

@end
