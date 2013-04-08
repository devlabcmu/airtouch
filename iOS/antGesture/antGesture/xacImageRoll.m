//
//  xacImageRoll.m
//  antGesture
//
//  Created by Xiang 'Anthony' Chen on 2/18/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacImageRoll.h"
#define MARGIN_PERCENTAGE 0.02

@implementation xacImageRoll

-(id) init
{
    self = [super init];
    _images = nil;
    _imageViews = [[NSMutableArray alloc] init];
    
    pvBackground = [[UIView alloc] init];
    CGRect screenRect = [[UIScreen mainScreen] bounds];
    _widthScreen = screenRect.size.width;
    _heightScreen = screenRect.size.height;
    
    pvBackground.frame = CGRectMake(0, 0, _widthScreen, _heightScreen);
    pvBackground.backgroundColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0.75];
    
    return self;
}

-(void) showImages :(int)numPerRow :(UIView*) parent
{
    if(_images == nil || numPerRow <= 0 || parent == nil) return;
 
    
    int numRows = _images.count / numPerRow + 1;
    int widthPerImage = parent.frame.size.width / numPerRow * (1 - MARGIN_PERCENTAGE * 2);
    float margin = (parent.frame.size.width - widthPerImage * numPerRow) / (numPerRow + 1);
    int heightOfTheRows = margin;
    
    for (int i = 0; i < numRows; i++) {
        int heightOfThisRow = 0;
        for (int j = 0; j < numRows; j++) {
            
            // load the image
            int idxImage = i * numPerRow + j;
            if(idxImage >= _images.count) break;
            NSString* nameImage = _images[idxImage];
            
            // set the image view
            UIImageView* imgView = [[UIImageView alloc] init];
            [_imageViews addObject:imgView];
            imgView.image = [UIImage imageNamed:nameImage];
            imgView.userInteractionEnabled = YES;
            int heightOfThisImage = imgView.image.size.height * widthPerImage / imgView.image.size.width;
            heightOfThisRow = MAX(heightOfThisRow, heightOfThisImage);
            
            // position the image view
            float x0 = margin * (j + 1) + widthPerImage * j;
            float y0 = heightOfTheRows;
            imgView.frame = CGRectMake(x0, y0, widthPerImage, heightOfThisImage);
            
            // add the image view to parent view
            [parent addSubview:imgView];
        }
        heightOfTheRows += (heightOfThisRow + margin);
    }
}

-(void) toogleZoom :(UIImageView*) imgView :(BOOL) toZoomIn
{
    int widthOld = imgView.frame.size.width;
    int heightOld = imgView.frame.size.height;
    float x0Old = imgView.frame.origin.x;
    float y0Old = imgView.frame.origin.y;
    
    if(toZoomIn)
    {
//        imgView.frame = CGRectMake(x0Old - widthOld * 0.5, y0Old - heightOld * 0.5, widthOld * 2, heightOld * 2);
        imgView.frame = CGRectMake(x0Old , y0Old , widthOld * 2, heightOld * 2);
    }
    else
    {
//        imgView.frame = CGRectMake(x0Old + widthOld * 0.25, y0Old + heightOld * 0.25, widthOld * 0.5, heightOld * 0.5);
        imgView.frame = CGRectMake(x0Old, y0Old, widthOld * 0.5, heightOld * 0.5);        
    }
}

bool isPreviewing = false;
- (void) doHitTest :(UITouch*) touch
{
    if(!isPreviewing)
    {
        for(UIImageView* iv in _imageViews)
        {
            UIImageView* imgView = (UIImageView*)[iv hitTest: [touch locationInView: iv] withEvent: NULL];
            if(imgView != NULL)
            {
                [self showPreview:imgView :iv.superview];
            }
    //            imgView.backgroundColor = [UIColor redColor];
        }
        
        isPreviewing = true;
    }
    else
    {
        [self hidePreview];
        isPreviewing = false;
    }
}

UIView* pvBackground = nil;
UIImageView* pvImgView = nil;
- (void) showPreview: (UIImageView*) imgView :(UIView*) container
{
    // add a background layer
    [container.superview addSubview:pvBackground];
    
    // clone the image view and resize, reposition it
//    pvImgView = (UIImageView*)[imgView copy];
    NSData *archivedData = [NSKeyedArchiver archivedDataWithRootObject: imgView];
    pvImgView = (UIImageView*)[NSKeyedUnarchiver unarchiveObjectWithData: archivedData];
    float widthPreview = _widthScreen * 0.8;
    float heightPreview = imgView.frame.size.height * widthPreview / imgView.frame.size.width;
    float xOrigin = (_widthScreen - widthPreview) * 0.5;
    float yOrigin = (_heightScreen - heightPreview) * 0.5;
    pvImgView.frame = CGRectMake(xOrigin, yOrigin, widthPreview, heightPreview);
    
    [pvBackground addSubview:pvImgView];
}

- (void) hidePreview
{
    [pvImgView removeFromSuperview];
    [pvBackground removeFromSuperview];
}

@end
