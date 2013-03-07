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
#import "xacTextSelection.h"
//#import "xacConstants.h"

#define DO_CONTEXT_MENU     TRUE
#define DO_SCROLLING        TRUE
#define DO_TEXT_SELECTION   TRUE

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

@property xacUIFade* uiFade;
@property xacUIScroll* uiScroll;
@property xacTextSelection* uiTextSel;
@property enum TextSelInteractState textSelIntState;

@property (weak, nonatomic) IBOutlet UIButton *btnSettings;
@property (weak, nonatomic) IBOutlet UIButton *btnLibrary;
@property (weak, nonatomic) IBOutlet UISlider *sldChapters;

- (IBAction)manuallyScroll:(id)sender;

@end
