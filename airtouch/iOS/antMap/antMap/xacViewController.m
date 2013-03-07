//
//  xacViewController.m
//  antMap
//
//  Created by Xiang 'Anthony' Chen on 3/6/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacViewController.h"

#define REQUEST_FREQUENCY   50
#define UI_UPDATE_FREQUENCY 100
#define PORT                10000
#define IP_CMU              "128.237.236.189"

#define INIT_LATI   40.443387
#define INIT_LONG   -79.945385
#define SPAN_LATI   0.112872
#define SPAN_LONG   0.109863

#define TIMEOUT_CIRCLE_ZOOM (kSamplePoints)

#define GESTURE_SAMPLE_INTERVAL 5

#define NUM_POINTS  3
#define AIR_BUFFER_SIZE 128

#define UNKNOW      -1
#define NONE        0
#define CIRCLE      1
#define CIRCLE_CW   2
#define CIRCLE_CCW  3

@interface xacViewController ()

@end

@implementation xacViewController

long counterSampling = 0;
int counterCircleZoom = TIMEOUT_CIRCLE_ZOOM;
int counterGestureRate = 0;
double timerGestureRate = 0;
//int counterDataRate = 0;
//double timerDataRate = 0;


float xBuf[NUM_POINTS];
float yBuf[NUM_POINTS];
int ptrBuf = 0;


- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
 
    _stream = nil;
    _airData = [[xacData alloc] init:AIR_BUFFER_SIZE];
    
    [NSTimer scheduledTimerWithTimeInterval:1.0 / REQUEST_FREQUENCY
                                     target:self
                                   selector:@selector(requestCamData)
                                   userInfo:nil
                                    repeats:YES];
    
    [_mapView setDelegate:self];
    _mapView.scrollEnabled = YES;
    [self setLocation:INIT_LATI :INIT_LONG :SPAN_LATI :SPAN_LONG :YES];
    
    if(SHOW_GESTURE)
    {
        _curveView = [[xacCurve alloc] initWithFrame:CGRectMake(0, 0, WIDTH_SCREEN, HEIGHT_SCREEN)];
        [_curveView setBackgroundColor: [UIColor colorWithRed:255 green:255 blue:255 alpha:0]];
    }
    
    UITapGestureRecognizer *singleTapGestureRecognizer = [[UITapGestureRecognizer alloc]
                                                          initWithTarget:self
                                                          action:@selector(handleSingleTap:)];
    [self.view addGestureRecognizer:singleTapGestureRecognizer];
    
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
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)handleSingleTap:(UITapGestureRecognizer *)recognizer
{
//    [_curveView setNeedsDisplay];
//    _curveView.alpha = 1.0;
    counterCircleZoom = 0;
    cntCircles = 0;
    toStopGesture = false;
    [_recognizer resetTouches];
    [_lbGestureDirection setText:@""];
}

- (void)setLocation:(float) lati :(float)lgti :(float)sLati :(float)sLgti :(BOOL)toAnimate
{
    MKCoordinateRegion newRegion;
    newRegion.center.latitude = lati;
    newRegion.center.longitude = lgti;
    newRegion.span.latitudeDelta = sLati;
    newRegion.span.longitudeDelta = sLgti;
    
    [_mapView setRegion:newRegion animated:toAnimate];
}

- (void)updateUI
{
    if(_stream.isConnected)
    {
        xacVector* airCoord = _airData.vecRaw;
        float xCenter = [self crop: airCoord.rawX * WIDTH_SCREEN: 0: WIDTH_SCREEN];
        float yCenter = [self crop: airCoord.rawZ * HEIGHT_SCREEN: 0: HEIGHT_SCREEN];
        float height = [self crop:airCoord.rawY :0 :MAX_HEIGHT];
        
    }
}

- (IBAction)connect:(id)sender {
    if(_stream == nil)
    {
        //        _stream = [[xacNetworkStreaming alloc] init: _atp];
        _stream = [[xacNetworkStreaming alloc] init: _airData];
        _stream.ipAddr = IP_CMU;
        _stream.port = PORT;
        
        // visualize as trace
        if(SHOW_GESTURE) [_mainView addSubview:_curveView];
        
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

- (void)requestCamData {
    if(_stream.isConnected)
    {
        //        NSLog(@"sensor info sent");
        [_stream sendStrToServer:@"f"];
        [_lbDataRate setText:[NSString stringWithFormat:@"data: %d", _stream.fps]];

        if(SHOW_GESTURE) _curveView.alpha *= 0.9;
        if(!toStopGesture)
        {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self updateGesture];
            });
        }
        else
        {
            [_lbGestureDetected setText:@"stopped."];
        }

    }
}

bool toStopGesture = false;
- (void) updateGesture
{
    if(counterSampling % 5 == 0)
    {
        xacVector* airCoord = _airData.vecRaw;
        float xCenter = [self crop: airCoord.rawX * WIDTH_SCREEN: 0: WIDTH_SCREEN];
        float yCenter = [self crop: airCoord.rawZ * HEIGHT_SCREEN: 0: HEIGHT_SCREEN];
        
        xBuf[ptrBuf] = xCenter;
        yBuf[ptrBuf] = yCenter;
        ptrBuf++;
        
        if(SHOW_GESTURE)
        {
            if(ptrBuf >= NUM_POINTS)
            {
                [_curveView updateCurve:xBuf[0] :yBuf[0] :xBuf[1] :yBuf[1] :xBuf[2] :yBuf[2]];
//                if(counterCircleZoom < TIMEOUT_CIRCLE_ZOOM)
//                {
//                    
////                    counterCircleZoom++;
//                    
//                    
//                }
//                else
//                {
//                     _curveView.alpha *= 0.5;
//                }
                ptrBuf = 0;
            }
        }
        
        if(DO_GESTURE_RECOGNITION)
        {
            if(counterCircleZoom < TIMEOUT_CIRCLE_ZOOM)
            {
//                NSLog(@"adding points...");
                [_recognizer addAirPoint:CGPointMake(xCenter, yCenter)];
                if([_recognizer isReadyForRecognition])
                {
//                    NSLog(@"processing data...");
                    [self processGestureData];
                    
                    [_recognizer resetTouches];
                    
                    if(_gesture == CIRCLE_CW || _gesture == CIRCLE_CCW)
                    {
                        float zoomBase = 0.25;
                        if(_gesture == CIRCLE_CW) [self zoomMap:zoomBase];
                        else if(_gesture == CIRCLE_CCW) [self zoomMap:-zoomBase];
                        
                        if(SHOW_GESTURE)
                        {
                            [_curveView setNeedsDisplay];
                            _curveView.alpha = 1.0;
                        }
                        counterCircleZoom = 0;
                        return;
                        NSLog(@"reset counterCicleZoom");
                    }
                }
                else
                {
//                    NSLog(@"getting data... %d", counterCircleZoom);
                    [_lbGestureDetected setText:@"detecting..."];
                }
            }
            else
            {
                [_lbGestureDetected setText:@"time out..."];
            }
        }
        
        if(counterCircleZoom < TIMEOUT_CIRCLE_ZOOM)
        {
            counterCircleZoom++;    
            if(counterCircleZoom == TIMEOUT_CIRCLE_ZOOM)
            {
                if(SHOW_GESTURE)
                {
                    [_curveView setNeedsDisplay];
                    _curveView.alpha = 1.0;
                }
            }

        }
        
        
    }
    counterSampling++;
//    _curveView.alpha *= 0.99;
    
    
    counterGestureRate++;
    double now = CACurrentMediaTime();
    if(now - timerGestureRate > 1)
    {
        [_lbGestureRate setText:[NSString stringWithFormat:@"gesture: %d", counterGestureRate]];
        timerGestureRate = now;
        counterGestureRate = 0;
    }

}

CGPoint center;
float score;
float angle;

int cntCircles = 0;
NSString* strDir = @"";
- (void) processGestureData
{
	NSString *gestureName = [_recognizer findBestMatchCenter:&center angle:&angle score:&score];
    
    _gesture = UNKNOW;
    float diameter = [_recognizer getXDiameter];
    if(diameter < WIDTH_SCREEN * 0.2 )
    {
        toStopGesture = true;
        return;
    }
//    if([gestureName isEqualToString: @"circle"])
    {
        if(diameter < WIDTH_SCREEN * 0.333 || diameter > WIDTH_SCREEN * 0.8)
        {
            _gesture = NONE;
            gestureName = @"none";
        }
        else
        {
            if([gestureName isEqualToString: @"circle cw"]) _gesture = CIRCLE_CW;
            else if ([gestureName isEqualToString: @"circle ccw"]) _gesture = CIRCLE_CCW;
            cntCircles++;
        }
    }
//    else if([gestureName isEqualToString: @"tap"])
//    {
////        if(diameter > WIDTH_SCREEN * 0.333)
////        {
////            _gesture = CIRCLE;
////            gestureName = @"circle";
////            NSLog([NSString stringWithFormat:@"diameter == %f", diameter]);
////            cntCircles++;
////        }
////        else
//        {
//            _gesture = NONE;
//        }
//    }
    
    
    
    [_lbGestureDetected setText: [NSString stringWithFormat:@"%@ (%0.2f, %d)", gestureName, score, (int)(360.0f*angle/(2.0f*M_PI))]];
    NSLog([NSString stringWithFormat:@"%@ (%0.2f, %d)", gestureName, score, (int)(360.0f*angle/(2.0f*M_PI))]);
    
    
//    if(cntCircles == 1)
//    {
//        if([_recognizer isClockWise]) strDir = @"clockwise";
//        else strDir = @"counter clockwise";
//    }
    
    
    [_lbGestureDirection setText:[NSString stringWithFormat:@"%@, %@ x%d", gestureName, strDir, cntCircles]];
}

float sLati = SPAN_LATI;
float sLgti = SPAN_LONG;
- (void) zoomMap: (float)dZoom
{
    sLati *= (1 - dZoom);
    sLgti *= (1 - dZoom);
    
    [self setLocation: _mapView.centerCoordinate.latitude: _mapView.centerCoordinate.longitude: sLati: sLgti: NO];
    
    //    NSString* dataStr = [NSString stringWithFormat:@"%f, %f, %f", dZoom, _sLati, _sLgti];
    //    NSLog(@"%@", dataStr);
    
}

- (float) crop :(float) original :(float) lower :(float) higher
{
    if(original < lower) return lower;
    if(original > higher) return higher;
    return original;
}
@end
