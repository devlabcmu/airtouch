//
//  xacViewController.h
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 1/5/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "xacNetworkStreaming.h"
#import "xacAirTouchProfile.h"
#import "xacScreenTouchProfile.h"

@interface xacViewController : UIViewController

@property (strong, nonatomic) IBOutlet UIView *viewMain;
@property (weak, nonatomic) IBOutlet UIButton *btnConnect;
@property xacNetworkStreaming* stream;
@property xacAirTouchProfile* atp;
@property xacScreenTouchProfile* stp;

- (IBAction)connect:(id)sender;

@end