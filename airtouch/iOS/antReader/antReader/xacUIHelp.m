//
//  xacUIHelp.m
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 3/8/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacUIHelp.h"

@implementation xacUIHelp

int counterHover = 0;

- (id)init: (UIView*) parent
{
    self = [super init];
    
    if(self)
    {
        _dictTooltips = [[NSMutableDictionary alloc] init];
        _tooltip = [[xacTooltip alloc] init];
        [parent addSubview:_tooltip];
        _tooltip.alpha = 0;
    }
    
    return self;
}

- (void) update:(float)x :(float)y :(float)z
{
    float xOld = _xCenter;
    float yOld = _yCenter;
    
    _xCenter = _xCenter * SMOOTH_FACTOR + x * (1 - SMOOTH_FACTOR);
    _yCenter = _yCenter * SMOOTH_FACTOR + y * (1 - SMOOTH_FACTOR);
    
    _xCenter = MAX(RADIUS, _xCenter);
    _xCenter = MIN(_xCenter, WIDTH_SCREEN - RADIUS);
    _yCenter = MAX(RADIUS, _yCenter);
    _yCenter = MIN(_yCenter, HEIGHT_SCREEN - RADIUS);
    
    if(fabs(xOld - _xCenter) < THRS_HOVER_OFFSET && fabs(yOld - _yCenter) < THRS_HOVER_OFFSET)
    {
        counterHover++;
    }
    
    if(fabs(xOld - x) > THRS_HOVER_OFFSET * 5 || fabs(yOld - y) > THRS_HOVER_OFFSET * 5)
    {
        counterHover *= 0.1;
        if(_tooltip.alpha >= 1)
        {
            [UIView animateWithDuration:0.5 animations:^{
                _tooltip.alpha = 0.0;
            }];
        }
    }

    CGPoint origin = CGPointMake(_xCenter, _yCenter + (_yCenter - HEIGHT_SCREEN * 0.75) / HEIGHT_SCREEN * 10 * RADIUS);
    [_uiShadow directUpdate:origin.x :origin.y :z];
    
    if(counterHover >= TIMEOUT_HOVER)
    {
        counterHover = 0;

        if(_tooltip.alpha <= 0.01)
        {
//            NSLog(@"%f, %f", _xCenter, _yCenter);
            
            CGSize size = CGSizeMake(RADIUS * 8, RADIUS * 6);
            
            NSString* msg = [self retrieveTooltipMsg:origin];
            if(msg != nil)
            {
                [_tooltip show :origin :size :msg];
                [UIView animateWithDuration:0.5 animations:^{
                    _tooltip.alpha = 1;
                }];
            }
        }
        else NSLog(@"%f", _tooltip.alpha);
    }
//    else NSLog(@"%d", counterHover);
}

- (void) createTooltip: (NSValue*) rect :(NSString*) text
{
    [_dictTooltips setObject:text forKey:rect];
}

- (NSString*) retrieveTooltipMsg :(CGPoint)pnt
{
    NSArray *keyArray =  [_dictTooltips allKeys];
    for(NSValue* key in keyArray)
    {
        CGRect bBox = [key CGRectValue];
        if(bBox.origin.x < pnt.x && pnt.x < bBox.origin.x + bBox.size.width &&
           bBox.origin.y < pnt.y && pnt.y < bBox.origin.y + bBox.size.height)
        {
            return (NSString*)[_dictTooltips objectForKey:key];
        }
    }
    return nil;
}

@end
