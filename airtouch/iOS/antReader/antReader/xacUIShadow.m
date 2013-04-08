//
//  xacUIShadow.m
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 3/8/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacUIShadow.h"
#import "xacConstants.h"

@implementation xacUIShadow

float flagVisibility = 0;


- (id) init :(UIView*) parentView
{
    self = [super init];
    if(self)
    {
        _circleView = [[UIView alloc] init];
        _circleView.backgroundColor = [UIColor redColor];
        [parentView addSubview: _circleView];
    }
    return self;
}

float radius = 0;
float xCenter = 0;
float yCenter = 0;
- (void) update:(float)x :(float)y :(float)z
{
    float smoothFactor = 0.95;
    float heightRatio = (z - MIN_HEIGHT) / (MAX_HEIGHT - MIN_HEIGHT);
    radius = radius * smoothFactor + (1 + heightRatio) * BASE_RADIUS * (1 - smoothFactor);
    xCenter = xCenter * smoothFactor + x * (1 - smoothFactor);
    yCenter = yCenter * smoothFactor + y * (1 - smoothFactor);
    
    xCenter = MAX(radius, xCenter);
    xCenter = MIN(xCenter, WIDTH_SCREEN - radius);
    yCenter = MAX(radius, yCenter);
    yCenter = MIN(yCenter, HEIGHT_SCREEN - radius);
    
    _circleView.alpha = (0.75 * (1 - heightRatio) + 0.05) * flagVisibility;
    _circleView.frame = CGRectMake(0, 0, 2 * radius, 2 * radius);
    _circleView.layer.cornerRadius = radius;
    [_circleView setCenter:CGPointMake(xCenter, yCenter + (yCenter - HEIGHT_SCREEN * 0.75) / HEIGHT_SCREEN * 10 * radius)];// - radius * 7.5 * yCenter / HEIGHT_SCREEN)];
}

- (void) directUpdate: (float)x :(float)y :(float)z
{
    float smoothFactor = 0.90;
    float heightRatio = (z - MIN_HEIGHT) / (MAX_HEIGHT - MIN_HEIGHT);
    radius = radius * smoothFactor + (1 + heightRatio) * BASE_RADIUS * (1 - smoothFactor);
    
    _circleView.alpha = (0.75 * (1 - heightRatio) + 0.05) * flagVisibility;
    _circleView.frame = CGRectMake(0, 0, 2 * radius, 2 * radius);
    _circleView.layer.cornerRadius = radius;
    [_circleView setCenter:CGPointMake(x, y)];
}

- (void) setVisibility :(BOOL)toBeVisible
{
    flagVisibility = toBeVisible ? 1.0 : 0.0;
}

- (CGPoint) getShadowCenter
{
    return CGPointMake(xCenter, yCenter);
}

@end
