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
#import "xacUIScroll.h"
#import "xacUIShadow.h"
#import "xacTextSelection.h"
#import "xacUIHelp.h"
//#import "xacConstants.h"

#define DO_CONTEXT_MENU     TRUE
#define DO_SCROLLING        TRUE
#define DO_TEXT_SELECTION   TRUE
#define SHOW_FINGER_SHADOW  TRUE
#define DO_TOOLTIP          TRUE

@interface xacViewController : UIViewController

enum TextSelInteractState
{
    NO_SELECTION,
    SELECTION_STARTED,
    SELECTION_FINISHED
};

@property xacData* airData;
@property xacNetworkStreaming* stream;

@property (weak, nonatomic) IBOutlet UIButton *btnConnect;

- (IBAction)connect:(id)sender;
@property (weak, nonatomic) IBOutlet UIView *ctrlView;
@property (weak, nonatomic) IBOutlet UITextView *textView;
@property (weak, nonatomic) IBOutlet UIScrollView *scrollView;
@property (strong, nonatomic) IBOutlet UIView *mainView;

@property xacUIFade* uiFade;
@property xacUIScroll* uiScroll;
@property xacTextSelection* uiTextSel;
@property xacUIShadow* uiShadow;
@property xacUIHelp* uiHelp;
@property enum TextSelInteractState textSelIntState;

@property (weak, nonatomic) IBOutlet UIButton *btnSettings;
@property (weak, nonatomic) IBOutlet UIButton *btnLibrary;
@property (weak, nonatomic) IBOutlet UISlider *sldChapters;

- (IBAction)manuallyScroll:(id)sender;

@end
