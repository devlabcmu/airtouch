//
//  xacUIFade.m
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 2/12/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#define THRS_WIDTH_RATIO 0.75
#define THRS_HEIGHT_RATIO 0.75

#define FADE_TIME_OUT 100
#import "xacUIFade.h"

@implementation xacUIFade

int fadeCounter = 0;

- (id) init
{
    self = [super init];
    _uiCtrls = [[NSMutableArray alloc] init];    
    return self;
}

- (void) update :(float) val
{
    for(UIControl* uic in _uiCtrls)
    {
        uic.alpha = val;
    }
}

- (void) update :(float) x :(float) y :(float)z
{
    float xEngagement = fabs(x - WIDTH_SCREEN / 2) / (WIDTH_SCREEN / 2 * THRS_WIDTH_RATIO);
    float yEngagement = fabs(y - HEIGHT_SCREEN / 2) / (HEIGHT_SCREEN / 2 * THRS_HEIGHT_RATIO);
    float heightRatio = z / MAX_HEIGHT;
    
//    float fadeInValue = (1 - sqrt(xEngagement * xEngagement + yEngagement * yEngagement) * 0.5) * (1 - heightRatio);
    if(fadeCounter <= 0)
    {
        for(UIControl* uic in _uiCtrls)
        {
            uic.alpha *= 0.9;
            if(xEngagement <= 1 && yEngagement <= 1)
            {
                uic.alpha = (1 - sqrt(xEngagement * xEngagement + yEngagement * yEngagement) * 0.5) * (1 - heightRatio);
            }
        }
        
        if(xEngagement <= 1 && yEngagement <= 1)
        {
            if(xEngagement > 0.5 || yEngagement > 0.5)
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
