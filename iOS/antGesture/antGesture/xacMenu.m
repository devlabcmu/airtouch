//
//  xacMenu.m
//  antGesture
//
//  Created by Xiang 'Anthony' Chen on 2/19/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacMenu.h"

@implementation xacMenu

- (id) init
{
    self = [super init];
    if (self) {
        // Initialization code
        _buttons = [[NSMutableArray alloc] init];
        CGRect screenRect = [[UIScreen mainScreen] bounds];
        _widthScreen = screenRect.size.width;
        _heightScreen = screenRect.size.height;
        self.alpha = 0;
    }
    return self;
}

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
        _buttons = [[NSMutableArray alloc] init];
        CGRect screenRect = [[UIScreen mainScreen] bounds];
        _widthScreen = screenRect.size.width;
        _heightScreen = screenRect.size.height;
        self.alpha = 0;
    }
    return self;
}

- (void) addButton :(NSString*) label
{
    UIButton* uib = [[UIButton alloc] init];
    [uib setTitle:label forState:UIControlStateNormal];
    [_buttons addObject:uib];
//    [self addSubview:uib];
}

- (void) showMenu :(float)x0 :(float)y0 :(float)w :(float)h
{
    self.alpha = 1.0 - self.alpha;
    
    self.frame = CGRectMake(x0 + w > _widthScreen ? _widthScreen - w : x0,
                            y0 + h > _heightScreen ? _heightScreen - h : y0,
                            w, h);
    
    
    [self setNeedsDisplay];
    
//    for(UIButton* uib in _buttons)
//    {
//        
//    }
}

- (void) hideMenu
{
    self.alpha = 0;
}

// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect
{
    if(_buttons.count <= 0 || self.alpha == 0) return;
    
    float widthMenu = self.frame.size.width;
    float heightMenu = self.frame.size.height;
    float marginRatio = 0.03;
    
    float widthButton = widthMenu * (1 - 2 * marginRatio);
    float heightButton = heightMenu * (1 - 2 * marginRatio) / _buttons.count;
    
    float marginWidth = (widthMenu - widthMenu) * 0.5;
    float marginHeight = (heightMenu - heightButton * _buttons.count) / (_buttons.count + 1);
    
    float xOrigin = marginWidth;
    float yOrigin = marginHeight;
    
    // Drawing code
    for(UIButton* uib in _buttons)
    {
        [self addSubview:uib];
        uib.frame = CGRectMake(xOrigin, yOrigin, widthButton, heightButton);
        yOrigin += (heightButton + marginHeight);
    }
}


@end
