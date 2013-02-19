//
//  xacViewController.m
//  antGesture
//
//  Created by Xiang 'Anthony' Chen on 2/18/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacViewController.h"
#define BASE_RADIUS 25
#define PORT 10000
#define IP_CMU "128.237.125.31"
#define IP_874 "192.168.8.112"
#define REQUEST_FREQUENCY 0.01
#define AIR_BUFFER_SIZE 128
#define NUM_POINTS 3
#define MAX_HEIGHT 0.06

@interface xacViewController ()

@end

@implementation xacViewController

float xBuf[NUM_POINTS];
float yBuf[NUM_POINTS];
int ptrBuf = 0;

int widthScreen = -1;
int heightScreen = -1;

long cntrTime = 0;

CGPoint center;
float score;
float angle;

bool doGestureRecognition = true;

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
    
    CGRect screenRect = [[UIScreen mainScreen] bounds];
    widthScreen = screenRect.size.width;
    heightScreen = screenRect.size.height;
    
    _airData = [[xacData alloc] init:AIR_BUFFER_SIZE];
    
    _stream = nil;
    
    [NSTimer scheduledTimerWithTimeInterval:REQUEST_FREQUENCY
                                     target:self
                                   selector:@selector(sendSensorRequestToServer)
                                   userInfo:nil
                                    repeats:YES];
    
    _circleView = [[UIView alloc] initWithFrame:CGRectMake(0,0,0,0)];
    _circleView.alpha = 0;
    _circleView.layer.cornerRadius = 0;
    _circleView.backgroundColor = [UIColor redColor];
    
    _curveView = [[xacCurve alloc] initWithFrame:CGRectMake(0, 0, widthScreen, heightScreen)];
    [_curveView setBackgroundColor: [UIColor colorWithRed:255 green:255 blue:255 alpha:0]];
    
    _recognizer = [[GLGestureRecognizer alloc] init];
	NSData *jsonData = [NSData dataWithContentsOfFile:[[NSBundle mainBundle] pathForResource:@"Gestures" ofType:@"json"]];
    
    BOOL ok;
	NSError *error;
	ok = [_recognizer loadTemplatesFromJsonData:jsonData error:&error];
	if (!ok)
	{
		NSLog(@"Error loading gestures: %@", error);
//		self.caption = @"Error loading gestures.";
		return;
	}
    
    _imageRoll = [[xacImageRoll alloc] init];
    _imageRoll.images = [@[@"calgary00.jpg",
                         @"canada00.jpg",
                         @"canada01.jpg",
                         @"canadaGoose00.jpg",
                         @"canadaHall.jpg",
                         @"montreal.jpg",
                         @"nigara00.jpg",
                         @"ottawa00.jpg",
                         @"toronto00.jpg",
                         @"toronto01.jpg",
                         @"vancouver00.jpg",] mutableCopy];
    [_imageRoll showImages :3 :_mainView];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


- (void)sendSensorRequestToServer {
    //    NSLog(@"sensor info sent");
    
    if(_stream.isConnected)
    {
        [_stream sendStrToServer:@"f"];
        
        // visualize as a circle
        xacVector* airCoord = _airData.vecRaw;
        float xCenter = [self crop: airCoord.rawX * widthScreen: 0: widthScreen];
        float yCenter = [self crop: airCoord.rawZ * heightScreen: 0: heightScreen];
        
        float heightRatio = [self crop:airCoord.rawY :0 :MAX_HEIGHT] / MAX_HEIGHT;
        float radius = (1 + heightRatio) * BASE_RADIUS;
        
        _circleView.alpha = 0.75 * (1 - heightRatio) + 0.25;
        _circleView.frame = CGRectMake(0, 0, 2 * radius, 2 * radius);
        _circleView.layer.cornerRadius = radius;
        [_circleView setCenter:CGPointMake(xCenter, yCenter)];
        
        // visualize as trace
        
        if(cntrTime % 5 == 0)
        {
            
            xBuf[ptrBuf] = xCenter;
            yBuf[ptrBuf] = yCenter;
            ptrBuf++;
            
            if(ptrBuf >= NUM_POINTS)
            {
                [_curveView updateCurve:xBuf[0] :yBuf[0] :xBuf[1] :yBuf[1] :xBuf[2] :yBuf[2]];
                //                [_curveView setNeedsDisplay];
                ptrBuf = 0;
            }
            
            if(doGestureRecognition)
            {
                [_recognizer addAirPoint:CGPointMake(xCenter, yCenter)];
            }
            
            
        }
        cntrTime++;
        _curveView.alpha *= 0.99;
        
//        [recognizer addTouches:touches fromView:self];
        
//        NSString* strFPS = [NSString stringWithFormat:@"fps: %d", _stream.fps];
//        [_lbFPS setText:strFPS];
                
    }
}

- (float) crop :(float) original :(float) lower :(float) higher
{
    if(original < lower) return lower;
    if(original > higher) return higher;
    return original;
}

- (IBAction)connect:(id)sender {
    if(_stream == nil)
    {
        //        _stream = [[xacNetworkStreaming alloc] init: _atp];
        _stream = [[xacNetworkStreaming alloc] init: _airData];
        _stream.ipAddr = IP_CMU;
        _stream.port = PORT;
        
        // visualize as circle
        [_mainView addSubview:_circleView];
        
        // visualize as trace
        [_mainView addSubview:_curveView];
        
        // controls
        [_mainView addSubview:_ctrlView];
        [_ctrlView setBackgroundColor:[UIColor colorWithRed:255 green:255 blue:255 alpha:0]];
        
        
    }
    
    if(!_stream.isConnected)
    {
        [_stream connectToServer];
        _stream.isConnected = true;
        [_btnConnect setTitle:@"Disconnect" forState:UIControlStateNormal];
    }
    else
    {
        [_stream sendStrToServer:@"d"];
        [_circleView removeFromSuperview];
        [_curveView removeFromSuperview];
        _stream.isConnected = false;
        [_btnConnect setTitle:@"Connect" forState:UIControlStateNormal];
    }
}

//- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
//{
//
//}

bool isZoomed = false;
UIImageView* zoomedImgView;
- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    UITouch *touch = [[event allTouches] anyObject];
    
    UIImageView* imgView = (UIImageView*)touch.view;
    
    if(imgView != nil)
    {
//        CGPoint location = [touch locationInView: self.view];
//        imgView.center = location;
        
        if(!isZoomed)
        {
            
            if([touch.view isKindOfClass: UIImageView.class])
            {
                UIImageView* imgView = (UIImageView*)touch.view;
                //            NSLog(@"%@", imgView.image.);
                [_imageRoll toogleZoom :imgView :true];
                zoomedImgView = imgView;
                isZoomed = true;
            }
        }
        else
        {
            [_imageRoll toogleZoom:zoomedImgView :false];
            isZoomed = false;
        }

    }

    
//    for (UITouch *touch in touches)
//    {
//        if(!isZoomed)
//        {
//            UIImageView* imgView = (UIImageView*)touch.view;
//            if(imgView != nil)
//            {
//    //            NSLog(@"%@", imgView.image.);
//                [_imageRoll toogleZoom :imgView :true];
//                zoomedImgView = imgView;
//                isZoomed = true;
//            }
//        }
//        else
//        {
//            [_imageRoll toogleZoom:zoomedImgView :false];
//            isZoomed = false;
//        }
//        
//        break;
//    }
    
    if(_stream.isConnected)
    {
        if(doGestureRecognition)
        {
            [self processGestureData];
            [_recognizer resetTouches];
        }
        _curveView.alpha = 1.0;
        [_curveView setNeedsDisplay];
    }
}

- (void)processGestureData
{
	NSString *gestureName = [_recognizer findBestMatchCenter:&center angle:&angle score:&score];
    [_lbGesture setText: [NSString stringWithFormat:@"%@ (%0.2f, %d)", gestureName, score, (int)(360.0f*angle/(2.0f*M_PI))]];
}
@end
