//
//  xacUIToast.m
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 3/9/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacUIToast.h"

@implementation xacUIToast

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
        self.alpha = 0;
        [self setBackgroundColor:[UIColor colorWithRed:0 green:0 blue:0 alpha:0.50]];
        self.layer.cornerRadius = frame.size.width * 0.2;
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

- (void) setDisplay :(UIImage*) image
{
    [_imgView removeFromSuperview];
    _imgView = [[UIImageView alloc] initWithImage:image];
    float ratioMargin = 0.05;

    float marginHeight = self.frame.size.height * ratioMargin;
    float heightImgView = self.frame.size.height - marginHeight * 2;
    float widthImgView  = heightImgView * image.size.width / image.size.height;
    float marginWidth = (self.frame.size.width - widthImgView) * 0.5;
    
    [self addSubview:_imgView];
    _imgView.frame = CGRectMake(marginWidth, marginHeight, widthImgView, heightImgView);
}

- (void) toastOut:(float)sec
{
    [UIView animateWithDuration:sec animations:^{
        self.alpha = 1.0;
    }];
}

- (void) toastBack:(float)sec
{
    [UIView animateWithDuration:sec animations:^{
        self.alpha = 0.0;
    }];
}


@end
