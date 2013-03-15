//
//  xacUIScroll.h
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 2/12/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacUIBehavior.h"
#import "xacConstants.h"
#import <QuartzCore/QuartzCore.h>
#import "xacConstants.h"
#import "xacUIToast.h"

#define SCALE_SCROLL_SPEED 0.01
#define SCROLL_TIME_OUT 500

@interface xacUIScroll : xacUIBehavior

enum ScrollState
{
    S_DEFAULT,
    S_LOW,
    S_HIGH_UP,
    S_LOW_AGAIN
} ;

@property enum ScrollState scrollState;

@property UIScrollView* scrollView;
@property BOOL scrollEnabled;

- (id) init :(UIView*)parent;
- (void) printState;
- (void) setState :(enum ScrollState) state;
- (void) startScrolling;
-(void) manuallyScroll;

@property BOOL scrollStarted;
@property float scrollOffset;
@property float contentOffset;
@property xacUIToast* uiToast;
@property UIView* parent;

@end
