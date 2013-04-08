//
//  xacUIFade.h
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 2/12/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacUIBehavior.h"
#import "xacConstants.h"

@interface xacUIFade : xacUIBehavior

@property BOOL toPause;
@property NSMutableArray* uiCtrls;

- (void) addUICtrl :(UIControl*) uic;

@end
