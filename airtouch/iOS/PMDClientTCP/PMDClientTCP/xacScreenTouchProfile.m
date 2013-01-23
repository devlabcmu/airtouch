//
//  xacTouchProfile.m
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 1/6/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacScreenTouchProfile.h"

@implementation xacScreenTouchProfile

- (id) init
{
    self = [super init];
    
    _points = [[NSMutableArray alloc] init];
    
    return self;
}

- (void) updateTouchPoints:(int)id :(float)x :(float)y
{
    xacTouchPoint* tmpPnt = [[xacTouchPoint alloc] init];
    tmpPnt.x = x;
    tmpPnt.y = y;
    
    if(_points.count < id + 1)
    {
        [_points addObject:tmpPnt];
    }
    else
    {
        [_points replaceObjectAtIndex:id withObject:tmpPnt];
    }
}

@end
