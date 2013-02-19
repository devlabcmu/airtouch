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
            imgView.image = [UIImage imageNamed:nameImage];
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

@end
