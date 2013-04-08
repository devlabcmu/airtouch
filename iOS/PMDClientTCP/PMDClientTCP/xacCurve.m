//
//  xacCurve.m
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 2/6/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacCurve.h"
#define LENGTH_CURVE kSamplePoints

@implementation xacCurve

int pointsCurve[LENGTH_CURVE * 2];
int cntr;
int ptrCurve;

- (id)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    cntr = 0;
    ptrCurve = 0;
    
    return self;
}

- (void)updateCurve :(float)x1 :(float)y1 :(float)x2 :(float)y2 :(float)x3 :(float)y3
{
    pointsCurve[ptrCurve] = x1;
    ptrCurve = (ptrCurve + 1) % (LENGTH_CURVE * 2);
    pointsCurve[ptrCurve] = y1;
    ptrCurve = (ptrCurve + 1) % (LENGTH_CURVE * 2);
    pointsCurve[ptrCurve] = x2;	
    ptrCurve = (ptrCurve + 1) % (LENGTH_CURVE * 2);
    pointsCurve[ptrCurve] = y2;
    ptrCurve = (ptrCurve + 1) % (LENGTH_CURVE * 2);
    pointsCurve[ptrCurve] = x3;
    ptrCurve = (ptrCurve + 1) % (LENGTH_CURVE * 2);
    pointsCurve[ptrCurve] = y3;
    ptrCurve = (ptrCurve + 1) % (LENGTH_CURVE * 2);
    
    cntr = cntr < LENGTH_CURVE * 2 ? cntr + 6 : cntr;
}

- (void)drawRect:(CGRect)rect {
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextSetLineWidth(context, 5.0);
    CGContextSetStrokeColorWithColor(context, [UIColor colorWithRed:255 green:0 blue:0 alpha:0.5].CGColor);
    CGContextBeginPath(context);
    
    int start = cntr < LENGTH_CURVE * 2 ? 0 : ptrCurve;
    CGContextMoveToPoint(context, pointsCurve[start], pointsCurve[start+1]);
    
    
    for (int i = 0; i + 6 < cntr && cntr >= 6; i += 6) {
        int idx = (start + i) % (LENGTH_CURVE * 2);
        CGContextAddCurveToPoint(context, pointsCurve[idx],
                                 pointsCurve[(idx + 1) % (LENGTH_CURVE * 2)],
                                 pointsCurve[(idx + 2) % (LENGTH_CURVE * 2)],
                                 pointsCurve[(idx + 3) % (LENGTH_CURVE * 2)],
                                 pointsCurve[(idx + 4) % (LENGTH_CURVE * 2)],
                                 pointsCurve[(idx + 5) % (LENGTH_CURVE * 2)]);
        
//        if(pointsCurve[idx] == 0 && pointsCurve[(idx + 1) % (LENGTH_CURVE * 2)] == 0)
//        {
//            int brk = 1;
//        }
//        
//        if(pointsCurve[(idx + 2) % (LENGTH_CURVE * 2)] == 0 && pointsCurve[(idx + 3) % (LENGTH_CURVE * 2)] == 0)
//        {
//            int brk = 1;
//        }
//        
//        if(pointsCurve[(idx + 4) % (LENGTH_CURVE * 2)] == 0 && pointsCurve[(idx + 5) % (LENGTH_CURVE * 2)] == 0)
//        {
//            int brk = 1;
//        }
        
//        if(i == cntr - 6)
//        {
//            NSLog(@"%@", [NSString stringWithFormat:@"%d: %d, %d, %d, %d, %d, %d", i, pointsCurve[idx], pointsCurve[idx+1], pointsCurve[idx+2], pointsCurve[idx+3], pointsCurve[idx+4], pointsCurve[idx+5]]);
//        }
    }
    
//    CGContextAddCurveToPoint(context,125,150,175,150,200,100);
//    CGContextAddCurveToPoint(context,225,50,275,75,300,200);
    CGContextStrokePath(context);
}

@end
