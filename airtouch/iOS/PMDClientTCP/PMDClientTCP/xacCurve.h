//
//  xacCurve.h
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 2/6/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "xacData.h"

@interface xacCurve : UIView

//- (id)initWithFrame:(CGRect)frame;
//- (void)drawRect:(CGRect)rect;

@property NSArray* points;
- (void)updateCurve :(float)x1 :(float)y1 :(float)x2 :(float)y2 :(float)x3 :(float)y3;

@end
