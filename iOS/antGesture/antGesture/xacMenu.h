//
//  xacMenu.h
//  antGesture
//
//  Created by Xiang 'Anthony' Chen on 2/19/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface xacMenu : UIView

@property float widthScreen;
@property float heightScreen;

@property NSMutableArray* buttons;

- (id) init;
- (void) addButton :(NSString*) label;
- (void) showMenu :(float)x0 :(float)y0 :(float)w :(float)h;

@end
