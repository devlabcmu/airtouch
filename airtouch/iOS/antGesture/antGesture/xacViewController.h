//
//  xacViewController.h
//  antGesture
//
//  Created by Xiang 'Anthony' Chen on 2/18/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <QuartzCore/QuartzCore.h>
#import "xacNetworkStreaming.h"
#import "xacData.h"
#import "xacCurve.h"
#import "GLGestureRecognizer.h"
#import "GLGestureRecognizer+JSONTemplates.h"
#import "xacImageRoll.h"
#import "xacMenu.h"

@interface xacViewController : UIViewController

@property (strong, nonatomic) IBOutlet UIView *mainView;
@property (weak, nonatomic) IBOutlet UIView *ctrlView;
@property (weak, nonatomic) IBOutlet UIView *imgRollView;
@property (weak, nonatomic) IBOutlet UILabel *lbGesture;
@property (weak, nonatomic) IBOutlet UIButton *btnConnect;

@property int gesture;

- (IBAction)connect:(id)sender;

@property xacNetworkStreaming* stream;
@property xacData* airData;

@property xacCurve* curveView;
@property UIView* circleView;

@property GLGestureRecognizer* recognizer;

@property xacImageRoll* imageRoll;
@property xacMenu* altMenu;


@end
