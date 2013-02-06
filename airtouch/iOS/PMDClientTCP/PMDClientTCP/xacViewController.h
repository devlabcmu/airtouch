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
//#import "xacScreenTouchProfile.h"
#import "xacNativeNetworking.h"

@interface xacViewController : UIViewController

@property (strong, nonatomic) IBOutlet UIView *viewMain;
@property (weak, nonatomic) IBOutlet UIButton *btnConnect;
@property (weak, nonatomic) IBOutlet UILabel *lbFPS;

@property xacNetworkStreaming* stream;

@property xacAirTouchProfile* atp;
//@property xacScreenTouchProfile* stp;

@property (strong, nonatomic) IBOutlet UIView *mainView;
@property UIView* circleView;

- (IBAction)connect:(id)sender;

@end
