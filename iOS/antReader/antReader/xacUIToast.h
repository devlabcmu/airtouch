//
//  xacUIToast.h
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 3/9/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <QuartzCore/QuartzCore.h>

@interface xacUIToast : UIView

@property UIImageView* imgView;

- (void) setDisplay :(UIImage*) image;
- (void) toastOut:(float)sec;
- (void) toastBack:(float)sec;

@end
