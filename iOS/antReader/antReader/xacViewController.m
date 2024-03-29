//
//  xacViewController.m
//  antRange
//
//  Created by Xiang 'Anthony' Chen on 2/11/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacViewController.h"
#define PORT 10000
#define IP_CMU "128.237.126.216"
#define IP_874 "192.168.8.112"
#define REQUEST_FREQUENCY 0.02
#define UI_UPDATE_RATE 0.01
#define AIR_BUFFER_SIZE 128
#define NUM_POINTS 3


int widthScreen = -1;
int heightScreen = -1;

//int fadeCounter = 0;

@interface xacViewController ()

@end

@implementation xacViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    _airData = [[xacData alloc] init:AIR_BUFFER_SIZE];
    
    _stream = nil;
    
    [NSTimer scheduledTimerWithTimeInterval:REQUEST_FREQUENCY
                                     target:self
                                   selector:@selector(sendSensorInfoToServer)
                                   userInfo:nil
                                    repeats:YES];
    
    [NSTimer scheduledTimerWithTimeInterval:UI_UPDATE_RATE
                                     target:self
                                   selector:@selector(updateUI)
                                   userInfo:nil
                                    repeats:YES];
    
    CGRect screenRect = [[UIScreen mainScreen] bounds];
    widthScreen = screenRect.size.width;
    heightScreen = screenRect.size.height;
    
    [_textView setScrollEnabled:YES];
    _textView.editable = NO;

    [_textView setDelegate:self];
    
    _uiFade = [[xacUIFade alloc] init];
    [_uiFade addUICtrl:_btnLibrary];
    [_uiFade addUICtrl:_btnSettings];
    [_uiFade addUICtrl:_sldChapters];
    
    _uiScroll = [[xacUIScroll alloc] init: _mainView];
    _uiScroll.scrollView = _textView;
    
    // text selection
    _uiTextSel = [[xacTextSelection alloc] init];
    [_uiTextSel initSelection:_textView];
    _textSelIntState = NO_SELECTION;
    
    // finger shadow
    _uiShadow = [[xacUIShadow alloc] init: _mainView];
    
    // tooltip
    _uiHelp = [[xacUIHelp alloc] init: _mainView];
    _uiHelp.uiShadow = _uiShadow;
    
    
    UITapGestureRecognizer *singleTapGestureRecognizer = [[UITapGestureRecognizer alloc]
                                                          initWithTarget:self
                                                          action:@selector(handleSingleTap:)];
    [self.view addGestureRecognizer:singleTapGestureRecognizer];
}

float yStartScrolling = -1;
float tScroll = 0;
- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView
{
    _uiScroll.scrollStarted = false;
    CGPoint pntOffset = [_textView.layer.presentationLayer bounds].origin;
    yStartScrolling = pntOffset.y;
    tScroll = 0;
    
//    dispatch_async(dispatch_get_main_queue(), ^{
//        [self sendSensorInfoToServer];
////        [self updateUI];
//    });

}
//
- (void)scrollViewWillEndDragging:(UIScrollView *)scrollView withVelocity:(CGPoint)velocity targetContentOffset:(inout CGPoint *)targetContentOffset
{
//    _uiScroll.scrollState = LEFT_SURFACE;
//        _uiScroll.scrollState = LEFT_SURFACE;
//    NSLog(@"drag");
    //    NSLog(@"%f", dirScroll / (tScroll + 1));
//    NSLog(@"%f", tScroll);
//    float thresScrollTime = 15;
//    if(tScroll < thresScrollTime)
    
    if(tScroll > 20)
    {
        _uiScroll.scrollEnabled = TRUE;
        CGPoint pntOffset = [_textView.layer.presentationLayer bounds].origin;
        _uiScroll.scrollOffset = pntOffset.y - yStartScrolling;
        _uiScroll.contentOffset = _textView.contentOffset.y;
        NSLog(@"%f", tScroll);
        
        
        
        dispatch_async(dispatch_get_main_queue(), ^{
            //        [self sendSensorInfoToServer];
            [_uiScroll manuallyScroll];
        });
    }
//    else 
    
//    dispatch_async(dispatch_get_main_queue(), ^{
//        [self sendSensorInfoToServer];
////        [self updateUI];
////        [_uiScroll manuallyScroll];
//    });
    
//    [self updateUI];
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView
{
    if(!_uiScroll.scrollStarted)
    {
        [_uiScroll startScrolling];
    }
    
    tScroll++;
    
   
//    dispatch_async(dispatch_get_main_queue(), ^{
//        [self sendSensorInfoToServer];
////        [self updateUI];
//    });
    
}

bool selectionStarted = false;
UITextPosition* headSelection = nil;
- (void)handleSingleTap:(UITapGestureRecognizer *)recognizer {
    
    /*-- handle text selection --*/
    CGPoint location = [recognizer locationInView:_textView];
    
    if([_uiTextSel isTimeOut]) _textSelIntState = NO_SELECTION;
    
    switch (_textSelIntState) {
        case NO_SELECTION:
            [_uiTextSel startSelection:location];
            _textSelIntState = SELECTION_STARTED;
            NSLog(@"selection started.");
            break;
        case SELECTION_STARTED:
            [_uiTextSel finishSelection:location];
            _textSelIntState = NO_SELECTION;
            NSLog(@"selection finished.");
            break;
        default:
            // do nothing
            break;
    }
    
    /*-- handle scrolling --*/
    _uiScroll.scrollEnabled = FALSE;
    

}


- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

/*
 connect/disconnect to the server
 */
- (IBAction)connect:(id)sender {
    
    if(_stream == nil)
    {
        //        _stream = [[xacNetworkStreaming alloc] init: _atp];
        _stream = [[xacNetworkStreaming alloc] init: _airData];
        _stream.ipAddr = IP_CMU;
        _stream.port = PORT;
        
        if(SHOW_FINGER_SHADOW) [_uiShadow setVisibility:TRUE];
        
        if(DO_TOOLTIP)
        {
            [_uiHelp createTooltip:[NSValue valueWithCGRect:_btnLibrary.frame] :@"accessing the library of eBooks."];
            [_uiHelp createTooltip:[NSValue valueWithCGRect:_btnSettings.frame] :@"change the settings of the reader."];
            [_uiHelp createTooltip:[NSValue valueWithCGRect:_sldChapters.frame] :@"you are on page 250."];
        }
            
    }
    
    if(!_stream.isConnected)
    {
        [_stream connectToServer];
        _stream.isConnected = true;
//        _ctrlView.alpha = 0;
        [_btnConnect setTitle:@"Disconnect" forState:UIControlStateNormal];
    }
    else
    {
        [_stream sendStrToServer:@"d"];
        _stream.isConnected = false;
//        _ctrlView.alpha = 1;
        [_btnConnect setTitle:@"Connect" forState:UIControlStateNormal];
    }
}

/*
 requesting sensor data from the server
 */
- (void)sendSensorInfoToServer {
    {
//        NSLog(@"sensor info sent");
        [_stream sendStrToServer:@"f"];
    }
}

- (void)updateUI
{
    if(_stream.isConnected && _airData.isValid)
    {
        xacVector* airCoord = _airData.vecRaw;
        float xCenter = [self crop: airCoord.rawX * WIDTH_SCREEN: 0: WIDTH_SCREEN];
        float yCenter = [self crop: airCoord.rawZ * HEIGHT_SCREEN: 0: HEIGHT_SCREEN];
        float height = [self crop:airCoord.rawY :0 :MAX_HEIGHT];
        
        if(DO_CONTEXT_MENU)[_uiFade update:xCenter :yCenter :height];
        
        if(DO_SCROLLING)[_uiScroll update:xCenter :yCenter :height];
        
        if(DO_TEXT_SELECTION) [_uiTextSel update:xCenter :yCenter :height];
        
//        if(SHOW_FINGER_SHADOW) [_uiShadow update:xCenter :yCenter :height];
        
        if(DO_TOOLTIP) [_uiHelp update:xCenter :yCenter :height];
    }
}


- (float) crop :(float) original :(float) lower :(float) higher
{
    if(original < lower) return lower;
    if(original > higher) return higher;
    return original;
}

@end


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// AWARD_WINNING_CODE
//

//
//- (void) touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
//{
//    _uiFade.toPause = TRUE;
//}
//
//- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
//{
//    _uiFade.toPause = FALSE;
//    _uiScroll.scrollState = LEFT_SURFACE;
//}
//int scrollHeight = 0;
//- (IBAction)manuallyScroll:(id)sender {
    //    scrollHeight += 30;
    //    CGPoint bottomOffset = CGPointMake(0, scrollHeight);
    //    [_textView setContentOffset:bottomOffset animated:YES];
    
    // Get current selected range , this example assumes is an insertion point or empty selection
    //    UITextRange *selectedRange = [_textView selectedTextRange];
    //
    //
    //    // Calculate the new position, - for left and + for right
    //    UITextPosition *newPosition = [_textView positionFromPosition:selectedRange.start offset:150];
    //
    //    // Construct a new range using the object that adopts the UITextInput, our textfield
    //    UITextRange *newRange = [_textView textRangeFromPosition:selectedRange.start toPosition:newPosition];
    //
    //    int posStart = [_textView offsetFromPosition:_textView.beginningOfDocument
    //                            toPosition:newRange.start];
    //    int posEnd = [_textView offsetFromPosition:_textView.beginningOfDocument
    //                                        toPosition:newRange.end];
    //
    //    NSLog(@"%d, %d", posStart, posEnd);
    //
    //    // Set new range
    //    [_textView setSelectedTextRange:newRange];
    //
    //    [_textView selectAll:self];
    //    [_textView select:_textView.selectedTextRange];
//}

//-(void)handleSwipeFrom:(UISwipeGestureRecognizer *)recognizer {
//    NSLog(@"Swipe received.");
//}

//    else
//    {
//        _textSelIntState = NO_SELECTION;
//        NSLog(@"unselected.");
//    }

//    if(!selectionStarted)
//    {
////        headSelection = [_textView closestPositionToPoint:location];
//        [_uiTextSel startSelection:location];
//        selectionStarted = true;
//    }
//    else
//    {
////        UITextPosition* endSelection = [_textView closestPositionToPoint:location];
////        UITextRange* rangeSelection = [_textView textRangeFromPosition: headSelection toPosition:endSelection];
////
////
////        [_textView setSelectedTextRange:rangeSelection];
////        [_textView select: _textView.selectedTextRange];
//
//        [_uiTextSel finishSelection:location];
//        selectionStarted = false;
//    }
//    NSLog(@"Tap Gesture Coordinates: %.2f %.2f", location.x, location.y);
//    NSString *tappedSentence = [self lineAtPosition:CGPointMake(location.x, location.y)];
//    NSLog(@"%@", tappedSentence);

//- (void)singleTapGestureCaptured:(UITapGestureRecognizer *)gesture
//- (void)handleSingleTap:(UITapGestureRecognizer *)recognizer
//{
//    CGPoint location = [recognizer locationInView:_textView];
//    NSLog(@"Tap Gesture Coordinates: %.2f %.2f", location.x, location.y);
//    NSString *tappedSentence = [self lineAtPosition:CGPointMake(location.x, location.y)];
//}

//- (NSString *)lineAtPosition:(CGPoint)position
//{
//    //eliminate scroll offset
////    position.y += _textView.contentOffset.y;
//    //get location in text from textposition at point
//    UITextPosition *tapPosition = [_textView closestPositionToPoint:position];
//    //fetch the word at this position (or nil, if not available)
//    UITextRange *textRange = [_textView.tokenizer rangeEnclosingPosition:tapPosition withGranularity:UITextGranularitySentence inDirection:UITextLayoutDirectionRight];
//    return [_textView textInRange:textRange];
//}

//- (void)singleTapGestureCaptured:(UITapGestureRecognizer *)gesture
//{
//    int brk = 0;
//}
//    UISwipeGestureRecognizer* uiSwipe = [[UISwipeGestureRecognizer alloc] initWithTarget:_textView action:handleScroll]
//    [uiSwipe setDirection:(UISwipeGestureRecognizerDirectionDown)];
//    [_textView addGestureRecognizer:uiSwipe];
//
//    UITapGestureRecognizer *singleTap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(handleSingleTap)];
////    singleTap.numberOfTapsRequired = 1;
//    [self.view addGestureRecognizer:singleTap];

//#define MAX_HEIGHT 0.06
//#define THRS_WIDTH_RATIO 0.75
//#define THRS_HEIGHT_RATIO 0.75
//#define FADE_TIME_OUT 100
