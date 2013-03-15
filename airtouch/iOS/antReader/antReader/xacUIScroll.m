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

enum ScrollState tmpScrollState = S_DEFAULT;

NSMutableArray* imagesToast;

@implementation xacUIScroll

- (id) init :(UIView*)parent
{
    self = [super init];
    _parent = parent;
    _scrollState = S_DEFAULT;
    [NSTimer scheduledTimerWithTimeInterval:SCROLL_FREQ
                                     target:self
                                   selector:@selector(manuallyScroll)
                                   userInfo:nil
                                    repeats:YES];
    
    float widthToast = WIDTH_SCREEN * 0.8;
    float heightToast = widthToast * 0.667;
    float xToast = (WIDTH_SCREEN - widthToast) * 0.5;
    float yToast = (HEIGHT_SCREEN - heightToast) * 0.5;
    _uiToast = [[xacUIToast alloc] initWithFrame:CGRectMake(xToast, yToast, widthToast, heightToast)];
    [_uiToast setBackgroundColor:[UIColor colorWithRed:255 green:255 blue:255 alpha:0]];
    _uiToast.alpha = 0;
    [_parent addSubview: _uiToast];
    imagesToast = [@[@"speed1.png", @"speed2.png", @"speed3.png", @"speed-down-1.png", @"speed-down-2.png", @"speed-down-3.png"] mutableCopy];
    
    return self;
}

NSString* strState = @"";
float lastY = -1;
float lastY2 = -1;
float thisY = -1;
float height = -1;

int counterScroll = SCROLL_TIME_OUT;
float heightEnhancement = 1;

#define MIN_ENHANCEMENT 1
#define MAX_ENHANCEMENT 3

- (void) update:(float)x :(float)y :(float)z
{
    heightEnhancement = MIN_ENHANCEMENT + (MAX_ENHANCEMENT - MIN_ENHANCEMENT) * (z - MIN_HEIGHT) / (MAX_HEIGHT - MIN_HEIGHT);
    _uiToast.alpha *= 0.99;
}

- (void) update2:(float)x :(float)y :(float)z
{
//    thisY = fabs(thisY - lastY2) > HEIGHT_SCREEN / 2 ? thisY : y;
//    lastY2 = thisY;
//    rate *= 0.99;
//    height = z;
    
    if(!_scrollStarted) return;
    
    if(counterScroll >= SCROLL_TIME_OUT)
    {
        _scrollState = S_DEFAULT;
        if(counterScroll == SCROLL_TIME_OUT)
        {
            NSLog(@"scroll time out...");
            counterScroll++;
        }
        return;
    }
    else    counterScroll++;
    
    float height = z;
    enum ScrollState tmpState = _scrollState;
    switch (_scrollState) {
        case S_DEFAULT:
            if(height < THRES_LOW) tmpState = S_LOW;
            if(_scrollState != tmpState) NSLog(@"LOW");
            break;
        case S_LOW:
            if(height > THRES_HIGH_UP) tmpState = S_HIGH_UP;
            if(_scrollState != tmpState)
            {   NSLog(@"HIGH_UP");     }
            break;
        case S_HIGH_UP:
            //            [self dynmSelect];
            counterScroll--;
            _scrollEnabled = NO;
            if(height < MIN_HEIGHT * 1.5) tmpState = S_LOW_AGAIN;
            // maybe add a time out
            if(_scrollState != tmpState) NSLog(@"LOW_AGAIN");
            break;
        case S_LOW_AGAIN:
//            _scrollEnabled = false;
            _scrollOffset *= 0.9;
            break;
        default:
            break;
    }
    
    _scrollState = tmpState;
    
//    [self manuallyScroll];

}

-(void) manuallyScroll
{
    float topEdge = _scrollView.contentOffset.y;
    if(topEdge <= 0)
    {
        return;
    }
    
    float bottomEdge = _scrollView.contentOffset.y + _scrollView.frame.size.height;
    if (bottomEdge >= _scrollView.contentSize.height) {
        return;
    }

    /*--- do the scrolling ---*/
    if(_scrollEnabled)
    {
        rateBase = fabs(_scrollOffset) * SCALE_SCROLL_SPEED;
        dirScroll = _scrollOffset > 0 ? 1 : -1;
        _contentOffset +=  rateBase * dirScroll * heightEnhancement;
        
        //        scrollHeight += offsetScroll;
        
        CGPoint bottomOffset = CGPointMake(0, _contentOffset);
        [_scrollView setContentOffset:bottomOffset animated:NO];
//        dispatch_async(dispatch_get_main_queue(), ^{
//            [_scrollView setContentOffset:bottomOffset animated:NO];
//        });
        
        //        NSLog(@"%@", [NSString stringWithFormat:@"%f, %f", _contentOffset, _scrollView.contentOffset.y]);
        
        int idxToastNew = (int)(3 * (heightEnhancement - MIN_ENHANCEMENT) / (MAX_ENHANCEMENT - MIN_ENHANCEMENT) - 0.1);
        int idxOff = dirScroll > 0 ? 0 : 3;
        [_uiToast setDisplay:[UIImage imageNamed:imagesToast[idxToastNew + idxOff]]];
        
        if(idxToastNew != idxToast)
        {
//            if(_uiToast.alpha < 0.001)
            {
                [_uiToast toastOut:0.5];
            }
        }
        else
        {
            _uiToast.alpha *= 0.99;
        }
        idxToast = idxToastNew;
    }
}
int idxToast = -1;

- (void) setState:(enum ScrollState)state
{
    _scrollState = state;
    tmpScrollState = state;
}


- (float) sign :(float)num
{
    if(num > 0) return 1;
    else if(num < 0) return -1;
    else return 0;
}

//bool scrollEnabled = NO;
int dirScroll = 0;
- (void) startScrolling
{
//    if(height > MAX_HEIGHT / 10)
    {
        _scrollStarted = TRUE;
    _scrollEnabled = NO;
    //    scrollHeight = pntOffset.y + _scrollView.layer.bounds.size.height;// * thisY > HEIGHT_SCREEN * 0.9 ? 1 : -1;
//    dirScroll = _offset > 0 ? 1 : -1;//thisY > HEIGHT_SCREEN * 0.75 ? 1 : -1;
    
//    _contentOffset = _scrollView.contentOffset.y;
        counterScroll = 0;
        _scrollState = S_DEFAULT;
    }
}


float rateBase = 0;
//float minScrollOffset = 0;

@end
//- (void) manuallyScroll
//{
//    [self doManuallyScroll1];
//}
//
//- (void) doManuallyScroll1
//{
//    float topEdge = _scrollView.contentOffset.y;
//    if(topEdge <= 0)
//    {
//        _scrollEnabled = NO;
//        counter = 0;
//        minScrollOffset = 0;
//    }
//    
//    float bottomEdge = _scrollView.contentOffset.y + _scrollView.frame.size.height;
//    if (bottomEdge >= _scrollView.contentSize.height) {
////        NSLog(@"reach end!");
//        _scrollEnabled = NO;
//        counter = 0;
//        minScrollOffset = 0;
//    }
//    
//   if(counter >= TIME_OUT)
//    {
//        _scrollEnabled = NO;
//        counter = 0;
//        minScrollOffset = 0;
//    }
//    
//    if(height > MAX_HEIGHT * 2)
//    {
//        _scrollEnabled = NO;
//        counter = 0;
//        minScrollOffset = 0;
//        return;
//    }
//    
//    if(counter > 0)
//    {
//        if(height < MAX_HEIGHT / 10)
//        {
//            _scrollEnabled = NO;
//            counter = 0;
//            minScrollOffset = 0;
//        }
//    }
//    
//    if(_scrollEnabled)
//    {
//        CGPoint pntOffset = [_scrollView.layer.presentationLayer bounds].origin;
//        scrollHeight = pntOffset.y;
////        float tmpScroll = rateBase * height / MAX_HEIGHT;
////        offsetScroll = offsetScroll * 0.9 + tmpScroll * 0.1;
//        offsetScroll = rateBase * (height - MIN_HEIGHT) / (MAX_HEIGHT - MIN_HEIGHT);
//        offsetScroll = MAX(0, offsetScroll);
//        offsetScroll = MIN(offsetScroll, rateBase);
//        
//        if(offsetScroll <= minScrollOffset)
//        {
//            offsetScroll = 0;
//            counter++;
//            minScrollOffset *= 0.9;
//        }
//        else
//        {
////            minScrollOffset *= 1.1;
////            minScrollOffset = minScrollOffset > rateBase / 2 ? minScrollOffset : minScrollOffset * 1.01;
//            
//            
//            float rate = 0.9;
//            minScrollOffset = minScrollOffset * rate + offsetScroll * (1 - rate);
//            
//            offsetScroll *= dirScroll;//thisY > HEIGHT_SCREEN * 0.75 ? -1 : 1;
////            NSLog(@"%@", [NSString stringWithFormat:@"%f, %f, %f", minScrollOffset, offsetScroll, thisY]);
//            scrollHeight += offsetScroll;
//            CGPoint bottomOffset = CGPointMake(0, scrollHeight);
//            dispatch_async(dispatch_get_main_queue(), ^{
//                [_scrollView setContentOffset:bottomOffset animated:NO];
//            });
//        }
//    }
//}


//
//  AWARD_WINNING_CODE
//

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

//-(void) printState
//{
//    NSString* strState = @"";
//    switch (_scrollState) {
//        case MIDDLE:
//            strState = @"middle";
//            break;
//        case LOW:
//            strState = @"low";
//            break;
//        case HIGH:
//            strState = @"high";
//            break;
//        case LOW_AGAIN:
//            strState = @"low again";
//        case DEFAULT:
//            strState = @"default";
//        default:
//            break;
//    }
//    NSLog(@"%@", strState);
//}

//float scrollHeight = 0;
//float rate = 0;
//- (void) manuallyScroll2
//{
//    if(lastY > 0 && thisY > 0 && fabs(thisY - lastY) >= THRS_Y)
//    {
//        float offsetScroll = 0 ;
//
//        if(_scrollState == S_LOW)
//        {
//            offsetScroll = -[self sign :(thisY - lastY)] * fabs(thisY - lastY) * .05 * rate;
//
//        }
//        else if(_scrollState == S_HIGH_UP)
//        {
//            offsetScroll = fabs(thisY - HEIGHT_SCREEN * 0.5) * 0.02 * rate;
//            offsetScroll = thisY > HEIGHT_SCREEN * 0.9 ? -offsetScroll : offsetScroll;
//
////            if(fabs(offsetScroll) < 20)
////            {
////                offsetScroll = 0;
////            }
//            NSLog(@"%@", [NSString stringWithFormat:@"%f, %f", thisY, offsetScroll]);
//
//        }
//
//        scrollHeight += offsetScroll;
//        CGPoint bottomOffset = CGPointMake(0, scrollHeight);
//        [_scrollView setContentOffset:bottomOffset animated:NO];
//    }
//    else
//    {
////        NSLog(@"%@", [NSString stringWithFormat:@"%.0f, %.0f", lastY, thisY]);
//    }
//    lastY = thisY;
//
//}
