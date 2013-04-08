//
//  xacUIHelp.h
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 3/8/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacUIBehavior.h"
#import "xacConstants.h"
#import "xacTooltip.h"
#import "xacUIShadow.h"

#define SMOOTH_FACTOR 0.95
#define RADIUS 25
#define TIMEOUT_HOVER 200
#define THRS_HOVER_OFFSET 10

@interface xacUIHelp : xacUIBehavior

@property NSMutableDictionary* dictTooltips;

@property float xCenter;
@property float yCenter;

@property xacTooltip* tooltip;

@property UIView* parent;

- (id)init: (UIView*) parent;
- (void) createTooltip: (id) uiElm :(NSString*) text;

@property xacUIShadow* uiShadow;
@end
