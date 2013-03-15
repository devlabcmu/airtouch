//
//  xacTooltip.m
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 3/8/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacTooltip.h"

@implementation xacTooltip

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
//        [self setBackgroundColor:[UIColor colorWithRed:32 green:32 blue:32 alpha:0.75]];
        _textView = [[UITextView alloc] init];
        [_textView setBackgroundColor:[UIColor colorWithRed:0 green:0 blue:0 alpha:0.75]];
        [_textView setTextColor:[UIColor whiteColor]];
        [_textView setFont:[UIFont fontWithName:@"ArialMT" size:24]];
        [self addSubview:_textView];

    }
    return self;
}

/*
// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect
{
    // Drawing code
}
*/

- (void) show :(CGPoint)origin :(CGSize)size :(NSString*) msg
{
    float xOrigin = origin.x - size.width / 2;
    xOrigin = MAX(0, xOrigin);
    xOrigin = MIN(xOrigin, WIDTH_SCREEN - size.width);

    float yOrigin = origin.y - size.height;
    yOrigin = MAX(0, yOrigin);
    yOrigin = MIN(yOrigin, HEIGHT_SCREEN - size.height);
    
    self.frame = CGRectMake(xOrigin, yOrigin, size.width, size.height);
    float marginWidth = size.width * RATIO_MARGIN;
    float marginHeight = size.height * RATIO_MARGIN;
    [_textView setText:msg];
    _textView.frame = CGRectMake(marginWidth, marginHeight, size.width - marginWidth * 2, size.height - marginHeight * 2);
    _textView.alpha = 1;

}
@end
