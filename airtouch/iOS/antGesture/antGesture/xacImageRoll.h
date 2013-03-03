//
//  xacImageRoll.h
//  antGesture
//
//  Created by Xiang 'Anthony' Chen on 2/18/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <UIKit/UIKit.h>
//#import "xacUIBehavior.h"

@interface xacImageRoll : UIView

@property float widthScreen;
@property float heightScreen;

@property NSMutableArray* images;
@property NSMutableArray* imageViews;

-(id) init;
-(void) showImages :(int)numPerRow :(UIView*) parent;
-(void) toogleZoom :(UIImageView*) imgView :(BOOL) toZoomIn;
- (void) doHitTest :(UITouch*) touch;

@end
