//
//  xacUIFade.m
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 2/12/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#define THRS_WIDTH_RATIO 1.05
#define THRS_HEIGHT_RATIO 1.05

#define FADE_TIME_OUT 100
#import "xacUIFade.h"

@implementation xacUIFade

int fadeCounter = 0;

- (id) init
{
    self = [super init];
    _uiCtrls = [[NSMutableArray alloc] init];
    _toPause = false;
    return self;
}

//- (void) update :(float) val
//{
//    for(UIControl* uic in _uiCtrls)
//    {
//        uic.alpha = val;
//    }
//}

BOOL isInRange = YES;
- (void) update :(float) x :(float) y :(float)z
{
    if(_toPause)
    {
        return;
    }
    
    float xEngagement = fabs(x - WIDTH_SCREEN / 2) / (WIDTH_SCREEN / 2 * THRS_WIDTH_RATIO);
    float yEngagement = fabs(y - HEIGHT_SCREEN / 2) / (HEIGHT_SCREEN / 2 * THRS_HEIGHT_RATIO);
    float heightRatio = (z - MIN_HEIGHT) / (MAX_HEIGHT - MIN_HEIGHT);
    
    BOOL prevInRange = isInRange;
    
    if(!isInRange)
    {
        if(xEngagement <= 1 && yEngagement <= 1 && heightRatio <= 0.5)
        {
            isInRange = YES;
        }
    }
    
    if(isInRange)
    {
        if(xEngagement > 1 || yEngagement > 1 || heightRatio > 0.5)
        {
            isInRange = NO;
        }
    }
    
    if(prevInRange != isInRange)
    {
        [UIView animateWithDuration:1.0 animations:^{
            for(UIControl* uic in _uiCtrls)
            {
                uic.alpha = isInRange ? 1.0 : 0.0;
            }
        }];
    }
    
}

- (void) update2 :(float) x :(float) y :(float)z
{
    if(_toPause)
    {
        return;
    }
    
    float xEngagement = fabs(x - WIDTH_SCREEN / 2) / (WIDTH_SCREEN / 2 * THRS_WIDTH_RATIO);
    float yEngagement = fabs(y - HEIGHT_SCREEN / 2) / (HEIGHT_SCREEN / 2 * THRS_HEIGHT_RATIO);
    float heightRatio = z / MAX_HEIGHT;
    
//    float fadeInValue = (1 - sqrt(xEngagement * xEngagement + yEngagement * yEngagement) * 0.5) * (1 - heightRatio);
    if(fadeCounter <= 0)
    {
        for(UIControl* uic in _uiCtrls)
        {
            uic.alpha *= 0.95;
            if(xEngagement <= 1)// && yEngagement <= 1)
            {
                uic.alpha = (1 - sqrt(xEngagement * xEngagement /*+ yEngagement * yEngagement) * 0.75*/)) * (1 - heightRatio);
            }
        }
        
        if(xEngagement <= 1)// && yEngagement <= 1)
        {
            if(xEngagement > 0.5)// || yEngagement > 0.5)
            {
                fadeCounter = FADE_TIME_OUT;
            }
        }
    }
    else
    {
        for(UIControl* uic in _uiCtrls)
        {
            uic.alpha *= 1.1;
        }
        
        fadeCounter--;
    }
}

- (void) addUICtrl:(UIControl *)uic
{
    [_uiCtrls addObject:uic];
}

@end
