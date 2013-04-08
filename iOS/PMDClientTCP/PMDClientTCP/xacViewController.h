//
//  xacViewController.h
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 1/5/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <QuartzCore/QuartzCore.h>
#import "xacNetworkStreaming.h"
#import "xacAirTouchProfile.h"
#import "xacData.h"
#import "xacCurve.h"

@interface xacViewController : UIViewController

@property (weak, nonatomic) IBOutlet UIButton *btnConnect;
@property (weak, nonatomic) IBOutlet UILabel *lbFPS;

@property xacNetworkStreaming* stream;

//@property xacAirTouchProfile* atp;
@property xacData* airData;

@property (strong, nonatomic) IBOutlet UIView *mainView;
@property (weak, nonatomic) IBOutlet UIView *ctrlView;
@property UIView* circleView;
@property xacCurve* curveView;

- (IBAction)connect:(id)sender;

@end
