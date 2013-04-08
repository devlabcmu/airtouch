//
//  xacTooltip.h
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 3/8/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "xacConstants.h"
#define RATIO_MARGIN 0.02

@interface xacTooltip : UIView

@property UITextView* textView;
- (void) show :(CGPoint)origin :(CGSize)size :(NSString*) msg;

@end
