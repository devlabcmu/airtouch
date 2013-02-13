//
//  xacViewController.h
//  antRange
//
//  Created by Xiang 'Anthony' Chen on 2/11/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <QuartzCore/QuartzCore.h>
#import "xacNetworkStreaming.h"
#import "xacData.h"
#import "xacUIBehavior.h"
#import "xacUIFade.h"
//#import "xacConstants.h"

@interface xacViewController : UIViewController

@property xacData* airData;
@property xacNetworkStreaming* stream;

@property (weak, nonatomic) IBOutlet UIButton *btnConnect;

- (IBAction)connect:(id)sender;
@property (weak, nonatomic) IBOutlet UIView *ctrlView;

@property xacUIFade* uiFade;
@property (weak, nonatomic) IBOutlet UIButton *btnSettings;
@property (weak, nonatomic) IBOutlet UIButton *btnLibrary;
@property (weak, nonatomic) IBOutlet UISlider *sldChapters;

@end
