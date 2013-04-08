//
//  xacTextSelection.h
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 3/4/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "xacUIBehavior.h"
#import "xacConstants.h"
#import "xacConstants.h"
#define TEXT_SEL_TIME_OUT 100

@interface xacTextSelection : xacUIBehavior

enum TextSelectionState
{
    TS_DEFAULT,
    TS_LOW,
    TS_HIGH_UP,
    TS_LOW_AGAIN
};

@property enum TextSelectionState textSelState;
@property UITextView* textView;
@property CGPoint pntAirSelStart;

@property CGPoint pntTouchSelStart;
@property CGPoint pntTouchSelEnd;

@property UITextPosition* headSelection;

@property BOOL selectionEnabled;

- (void) startSelection: (CGPoint) pntTouch;
- (void) finishSelection: (CGPoint) pntTouch;
- (void) initSelection :(UITextView*) textView;
- (BOOL) isTimeOut;

@end
