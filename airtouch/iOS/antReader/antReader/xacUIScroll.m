//
//  xacUIScroll.m
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 2/12/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacUIScroll.h"
#define TIME_OUT 10
#define THRS_WIDTH_RATIO 0.75
#define THRS_HEIGHT_RATIO 0.75

#define SCROLL_FREQ 0.01

#define THRS_Y 1

float thrsHight = 0.015;
float thrsLow = 0.005;

long counter = 0;

enum ScrollState tmpScrollState = DEFAULT;

@implementation xacUIScroll

- (id) init
{
    self = [super init];
    _scrollState = DEFAULT;
    [NSTimer scheduledTimerWithTimeInterval:SCROLL_FREQ
                                     target:self
                                   selector:@selector(manuallyScroll)
                                   userInfo:nil
                                    repeats:YES];
    return self;
}

NSString* strState = @"";
float lastY = -1;
float lastY2 = -1;
float thisY = -1;
float height = -1;
- (void) update:(float)x :(float)y :(float)z
{
    thisY = fabs(thisY - lastY2) > HEIGHT_SCREEN / 2 ? thisY : y;
    lastY2 = thisY;
    rate *= 0.99;
    height = z;
//    float xEngagement = fabs(x - WIDTH_SCREEN / 2) / (WIDTH_SCREEN / 2 * THRS_WIDTH_RATIO);
//    float yEngagement = fabs(y - HEIGHT_SCREEN / 2) / (HEIGHT_SCREEN / 2 * THRS_HEIGHT_RATIO);
//    
//    if( z > MAX_HEIGHT || z <= 0)
//    {
//        lastY = thisY;
////        NSLog(@"reset");
//        return;
//    }
//    
//// = [NSString stringWithFormat:@"unknown, %f", z];
//
//    
//    
//    if(counter > 0)
//    {
//        counter--;
//        return;
//    }
//    
//    switch (_scrollState) {
//        case DEFAULT:
//            strState = [NSString stringWithFormat:@"default, %f", z];
//            counter = 0;
//            rate = 0;
//            if(0 < z && z <= thrsLow)
//            {
//                tmpScrollState = LOW;
//
//                strState = [NSString stringWithFormat:@"low, %f", z];
//                
//            }
//            break;
//        case LOW:
//                            rate += 1;
////            [self manuallyScroll:30];
//            if(thrsLow < z && z <= thrsHight)
//            {
//                tmpScrollState = MIDDLE;
//                rate = 0;
//                strState = [NSString stringWithFormat:@"middle, %f", z];
//
//            }
//            break;
//        case MIDDLE:
//            if(0 < z && z <= thrsLow)
//            {
//                tmpScrollState = LOW;
//
//                strState = [NSString stringWithFormat:@"low, %f", z];
//                
//            }
//            if(thrsHight < z)
//            {
//                tmpScrollState = HIGH;
////                rate = 50;
//                strState = [NSString stringWithFormat:@"high, %f", z];
//            }
//            break;
//        case HIGH:
//            if(rate < 50)
//            {
//                rate += 2;
//            }
////            [self manuallyScroll:90];
//            if(0 < z && z <= (thrsLow + thrsHight) / 2)
//            {
//                tmpScrollState = LOW_AGAIN;
//                rate = 0;
//                strState = [NSString stringWithFormat:@"low again, %f", z];
//            }
//            break;
//        case LOW_AGAIN:
//            counter = TIME_OUT;
//            tmpScrollState = DEFAULT;
//            strState = [NSString stringWithFormat:@"default, %f", z];
//            break;
//        default:
//            strState = [NSString stringWithFormat:@"default, %f", z];
////            tmpScrollState = DEFAULT;
//            break;
//    }
//    
//    if(tmpScrollState != _scrollState)
//    {
//        _scrollState = tmpScrollState;
////        counter = 0;
//        NSLog(@"%@", [NSString stringWithFormat:@"%ld, %@", counter, strState]);
//    }
////    else if(_scrollState != DEFAULT)
////    {
////        NSLog(@"%@", strState);
////    }
//    
//    tmpScrollState = _scrollState;
//    
////    if(_scrollState != DEFAULT && counter < TIME_OUT)
////    {
////        counter++;
////        if(counter >= TIME_OUT)
////        {
////            _scrollState = DEFAULT;
////            NSLog(@"time out!");
////            NSLog(@"");
////            NSLog(@"");
////            NSLog(@"");
////                  
////        }
////    }
}

-(void) printState
{
    NSString* strState = @"";
    switch (_scrollState) {
        case MIDDLE:
            strState = @"middle";
            break;
        case LOW:
            strState = @"low";
            break;
        case HIGH:
            strState = @"high";
            break;
        case LOW_AGAIN:
            strState = @"low again";
        case DEFAULT:
            strState = @"default";
        default:
            break;
    }
    NSLog(@"%@", strState);
}

- (void) setState:(enum ScrollState)state
{
    _scrollState = state;
    tmpScrollState = state;
}

float scrollHeight = 0;
float rate = 0;
- (void) manuallyScroll2
{
    if(lastY > 0 && thisY > 0 && fabs(thisY - lastY) >= THRS_Y)
    {
        float offsetScroll = 0 ;
        
        if(_scrollState == LOW)
        {
            offsetScroll = -[self sign :(thisY - lastY)] * fabs(thisY - lastY) * .05 * rate;

        }
        else if(_scrollState == HIGH)
        {
            offsetScroll = fabs(thisY - HEIGHT_SCREEN * 0.5) * 0.02 * rate;
            offsetScroll = thisY > HEIGHT_SCREEN * 0.9 ? -offsetScroll : offsetScroll;
            
//            if(fabs(offsetScroll) < 20)
//            {
//                offsetScroll = 0;
//            }
            NSLog(@"%@", [NSString stringWithFormat:@"%f, %f", thisY, offsetScroll]);

        }
        

        
        scrollHeight += offsetScroll;
        CGPoint bottomOffset = CGPointMake(0, scrollHeight);
        [_scrollView setContentOffset:bottomOffset animated:NO];
    }
    else
    {
//        NSLog(@"%@", [NSString stringWithFormat:@"%.0f, %.0f", lastY, thisY]);
    }
    lastY = thisY;
    
}

- (float) sign :(float)num
{
    if(num > 0) return 1;
    else if(num < 0) return -1;
    else return 0;
}

//bool scrollEnabled = NO;
int dirScroll = 0;
- (void) startScrolling :(float)offset
{
//    if(height > MAX_HEIGHT / 10)
    {
    _scrollEnabled = YES;
    //    scrollHeight = pntOffset.y + _scrollView.layer.bounds.size.height;// * thisY > HEIGHT_SCREEN * 0.9 ? 1 : -1;
    dirScroll = offset > 0 ? 1 : -1;//thisY > HEIGHT_SCREEN * 0.75 ? 1 : -1;
    rateBase = fabs(offset) * 0.5;
    offsetScroll = 0;
    }
}


float rateBase = 0;
float stopHeight = 1;
float offsetScroll = 0;

- (void) manuallyScroll
{
//    if(height < MAX_HEIGHT / 10)
//    {
//        return;
//    }
    
   if(counter >= TIME_OUT)
    {
        _scrollEnabled = NO;
        counter = 0;
        stopHeight = 1;
    }
    
    if(height > MAX_HEIGHT * 2)
    {
        _scrollEnabled = NO;
        counter = 0;
        stopHeight = 1;
        return;
    }
    
    if(counter > 0)
    {
        if(height < MAX_HEIGHT / 10)
        {
            _scrollEnabled = NO;
            counter = 0;
            stopHeight = 1;
        }
    }
    
    if(_scrollEnabled)
    {
        CGPoint pntOffset = [_scrollView.layer.presentationLayer bounds].origin;
        scrollHeight = pntOffset.y;
//        float tmpScroll = rateBase * height / MAX_HEIGHT;
//        offsetScroll = offsetScroll * 0.9 + tmpScroll * 0.1;
        offsetScroll = rateBase * height / MAX_HEIGHT;
        
        if(offsetScroll <= stopHeight)
        {
            offsetScroll = 0;
            counter++;
            stopHeight *= 0.9;
        }
        else
        {
//            stopHeight *= 1.1;
//            stopHeight = stopHeight > rateBase / 2 ? stopHeight : stopHeight * 1.01;
            
            
            float rate = 0.9;
            stopHeight = stopHeight * rate + offsetScroll * (1 - rate);
            
            offsetScroll *= dirScroll;//thisY > HEIGHT_SCREEN * 0.75 ? -1 : 1;
            NSLog(@"%@", [NSString stringWithFormat:@"%f, %f, %f", stopHeight, offsetScroll, thisY]);
            scrollHeight += offsetScroll;
            CGPoint bottomOffset = CGPointMake(0, scrollHeight);
            dispatch_async(dispatch_get_main_queue(), ^{
                [_scrollView setContentOffset:bottomOffset animated:NO];
            });
        }
    }
}

@end
