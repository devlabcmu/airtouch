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

@interface xacUIScroll : xacUIBehavior

enum ScrollState
{
    DEFAULT,
    LOW,
    MIDDLE,
    HIGH,
    LOW_AGAIN
} ;

@property enum ScrollState scrollState;
@property BOOL scrollEnabled;

@property UIScrollView* scrollView;

- (void) printState;

- (void) setState :(enum ScrollState) state;

- (void) startScrolling :(float) dir;

- (void) manuallyScroll;

@end
