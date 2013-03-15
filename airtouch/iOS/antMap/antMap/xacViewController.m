//
//  xacViewController.m
//  antMap
//
//  Created by Xiang 'Anthony' Chen on 3/6/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacViewController.h"


@interface xacViewController ()

@end

@implementation xacViewController

long counterSampling = 0;
int counterCircleZoom = TIMEOUT_CIRCLE_ZOOM;
int counterGestureRate = 0;
double timerGestureRate = 0;

float xBuf[NUM_POINTS];
float yBuf[NUM_POINTS];
int ptrBuf = 0;

int counterModeSwitch = MODE_SWITCH_TIME_OUT;

float yTouch;

int idxToast = 0;
NSMutableArray* imagesToast;


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
    
    // ui
    [_mapView setDelegate:self];
    _mapView.scrollEnabled = YES;
    [self setLocation:INIT_LATI :INIT_LONG :SPAN_LATI :SPAN_LONG :YES];
    
    if(SHOW_GESTURE)
    {
        _curveView = [[xacCurve alloc] initWithFrame:CGRectMake(0, 0, WIDTH_SCREEN, HEIGHT_SCREEN)];
        [_curveView setBackgroundColor: [UIColor colorWithRed:255 green:255 blue:255 alpha:0]];
    }
    
    float widthToast = WIDTH_SCREEN * 0.3;
    float heightToast = widthToast * 0.8;
    float xToast = (WIDTH_SCREEN - widthToast) * 0.5;
    float yToast = (HEIGHT_SCREEN - heightToast) * 0.5;
    _uiToast = [[xacUIToast alloc] initWithFrame:CGRectMake(xToast, yToast, widthToast, heightToast)];
    [_mainView addSubview: _uiToast];
    imagesToast = [@[@"double-arrow.png", @"magnifying-glass.png"] mutableCopy];
    
//    lineView = [[UIView alloc] initWithFrame:CGRectMake(0, 200, self.view.bounds.size.width, 1)];
//    lineView.backgroundColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0.5];
//    [_mainView addSubview:lineView];
    
    _uiShadow = [[xacUIShadow alloc] init: _mainView];
    
    // gestures
    _cycleZoomState = CZ_DEFAULT;
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
    
    _modeSwtichingState = MS_DEFAULT;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

CGPoint centroid;
CGPoint endPoint;
- (void)handleSingleTap:(UITapGestureRecognizer *)recognizer
{
//    [_curveView setNeedsDisplay];
//    _curveView.alpha = 1.0;
    counterCircleZoom = 0;
    cntCircles = 0;
    toStopGesture = false;
    [_recognizer resetTouches];
    [_lbGestureDirection setText:@""];
    
    counterModeSwitch = 0;
    
    _cycleZoomState = CZ_DEFAULT;
    
    centroid = [recognizer locationInView:self.view];
    
//    if(modeSwitched)
//    {
//        _mapView.scrollEnabled = !_mapView.scrollEnabled;
//        
//        idxToast = (idxToast + 1) % imagesToast.count;
//        [_uiToast setDisplay:[UIImage imageNamed:imagesToast[idxToast]]];
//        [_uiToast toastOut:0.5];
//    }
    
//    [_uiToast toastBack:0.5];
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
//        xacVector* airCoord = _airData.vecRaw;
//        float xCenter = [self crop: airCoord.rawX * WIDTH_SCREEN: 0: WIDTH_SCREEN];
//        float yCenter = [self crop: airCoord.rawZ * HEIGHT_SCREEN: 0: HEIGHT_SCREEN];
//        float height = [self crop:airCoord.rawY :0 :MAX_HEIGHT];
        
        if(DO_GESTURE_RECOGNITION)
        {
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
        
        if(DO_HIGH_MODE_SWITCHING)
        {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self updateModes];
            });
        }
        
        if(SHOW_FINGER_SHADOW)
        {
            xacVector* airCoord = _airData.vecRaw;
            float xCenter = [self crop: airCoord.rawX * WIDTH_SCREEN: 0: WIDTH_SCREEN];
            float yCenter = [self crop: airCoord.rawZ * HEIGHT_SCREEN: 0: HEIGHT_SCREEN];
            float height = [self crop:airCoord.rawY :0 :MAX_HEIGHT];

            [_uiShadow update:xCenter :yCenter :height];
            
        }
        
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
        if(SHOW_FINGER_SHADOW) [_uiShadow setVisibility:TRUE];
        
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

        [self updateUI];

    }
}

bool toStopGesture = false;
UIView* lineView;
float checkPoint = DOT_PRODUCT_EPS;
CGPoint lastPoint;
- (void) updateGesture
{

    xacVector* airCoord = _airData.vecRaw;
    
    if(airCoord.rawY > THRES_HIGH_UP)
    {
        _cycleZoomState = CZ_DEFAULT;
    }
    
    float xCenter = [self crop: airCoord.rawX * WIDTH_SCREEN: 0: WIDTH_SCREEN];
    float yCenter = [self crop: airCoord.rawZ * HEIGHT_SCREEN: 0: HEIGHT_SCREEN];
    CGPoint thisPoint= CGPointMake(xCenter, yCenter);
    
    
    if( _cycleZoomState == CZ_ZOOMING)
    {
//        lineView.frame = CGRectMake(centroid.x, centroid.y, endPointNew.x - centroid.x, endPointNew.y - centroid.y);
        CGPoint thisVector = CGPointMake(thisPoint.x - centroid.x, thisPoint.y - centroid.y);
        CGPoint lastVector = CGPointMake(lastPoint.x - centroid.x, lastPoint.y - centroid.y);
        float dotCurrStart = (thisVector.x * lastVector.x + thisVector.y * lastVector.y)
        / (sqrt((thisVector.x * thisVector.x) + (thisVector.y * thisVector.y)) * sqrt((lastVector.x * lastVector.x) + (lastVector.y * lastVector.y)));
        
        double eps = 0.01;
//        if(fabs(fabs(dotCurrStart) - fabs(checkPoint)) < eps)
//        {
//            NSLog(@"rotate!");
//            checkPoint = 1 - checkPoint;
//        }
        
        float crossCurrStart = thisVector.x * lastVector.y - lastVector.x * thisVector.y > 0 ? 1 : -1;
        
        float dAngle = crossCurrStart * acos(dotCurrStart);
        NSLog(@"%f", dAngle);
        
        [self zoomMap:-dAngle / 5];
    }
    else if(counterSampling % 5 == 0)
    {
        
        if(SHOW_GESTURE)
        {
            xBuf[ptrBuf] = xCenter;
            yBuf[ptrBuf] = yCenter;
            ptrBuf++;
            if(ptrBuf >= NUM_POINTS)
            {
                [_curveView updateCurve:xBuf[0] :yBuf[0] :xBuf[1] :yBuf[1] :xBuf[2] :yBuf[2]];
                ptrBuf = 0;
            }
        }
        
        if(counterCircleZoom < TIMEOUT_CIRCLE_ZOOM)
        {
            //                NSLog(@"adding points...");
            [_recognizer addAirPoint:CGPointMake(xCenter, yCenter)];
            if([_recognizer isReadyForRecognition])
            {
                //                    NSLog(@"processing data...");
                
                [self processGestureData];
                [_recognizer resetTouches];
                
                switch (_cycleZoomState) {
                    case CZ_DEFAULT:
                        if(_gesture == CIRCLE_CW || _gesture == CIRCLE_CCW)
                        {
                            _cycleZoomState = CZ_ZOOMING;
                            NSLog(@"zoom activated");
                            endPoint = CGPointMake(xCenter, yCenter);
//                            [lineView setNeedsDisplay];
//                            counterCircleZoom = 0;
                        }
                        break;
                    case CZ_ACTIVATED:
                        // recall initial states
//                        NSLog(@"zoom activated");
                        _cycleZoomState = CZ_ZOOMING;
                        break;
                    case CZ_ZOOMING:
//                        counterCircleZoom = 0;
//                        lineView.frame = CGRectMake(centroid.x, centroid.y, endPoint.x - centroid.x, endPoint.y - centroid.y);

                        break;
                    default:
                        break;
                }
                
//                if(_gesture == CIRCLE_CW || _gesture == CIRCLE_CCW)
//                {
//                    float zoomBase = 0.25;
//                    if(_gesture == CIRCLE_CW) [self zoomMap:zoomBase];
//                    else if(_gesture == CIRCLE_CCW) [self zoomMap:-zoomBase];
//                    
//                    if(SHOW_GESTURE)
//                    {
//                        [_curveView setNeedsDisplay];
//                        _curveView.alpha = 1.0;
//                    }
//                    counterCircleZoom = 0;
//                    return;
//                    NSLog(@"reset counterCicleZoom");
//                }
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

    lastPoint = thisPoint;
}

BOOL modeSwitched = false;
- (void) updateModes
{
    if(counterModeSwitch >= MODE_SWITCH_TIME_OUT)
    {

        if(counterModeSwitch == MODE_SWITCH_TIME_OUT)
        {
            NSLog(@"mode switching time out...");
            counterModeSwitch++;
            modeSwitched = false;
//            _modeSwtichingState = MS_DEFAULT;
        }
//        [_uiToast toastBack:0.5];
        //return;
    }
    else    counterModeSwitch++;
    
    _uiToast.alpha *= 0.99;
    
    xacVector* airCoord = _airData.vecRaw;
    float height = airCoord.rawY;
    
    enum ModeSwitchingState tmpState = _modeSwtichingState;
    
    switch (_modeSwtichingState) {
        case MS_DEFAULT:
            if(height < THRES_LOW) tmpState = MS_LOW;
            if(_modeSwtichingState != tmpState) NSLog(@"LOW");
            break;
        case MS_LOW:
            if(height > THRES_HIGH_UP) tmpState = MS_HIGH_UP;
            if(_modeSwtichingState != tmpState)
            {
                NSLog(@"HIGH_UP");
                toStopGesture = true;
                
                modeSwitched = true;
                counterModeSwitch = 0;
            }
            break;
        case MS_HIGH_UP:
//            [self dynmSelect];
            if(height < MIN_HEIGHT * 1.5) tmpState = MS_LOW_AGAIN;
            // maybe add a time out
            if(_modeSwtichingState != tmpState)
            {
                NSLog(@"LOW_AGAIN");
            }
            break;
        case MS_LOW_AGAIN:
            //            _selectionEnabled = true;
            tmpState = MS_DEFAULT;
            break;
        default:
            break;
    }
    
    _modeSwtichingState = tmpState;
    
}

/* --- --- handling touch --- --- */
- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    _cycleZoomState = CZ_DEFAULT;
    
    if(modeSwitched)
    {
        _mapView.scrollEnabled = !_mapView.scrollEnabled;
        
        idxToast = (idxToast + 1) % imagesToast.count;
        [_uiToast setDisplay:[UIImage imageNamed:imagesToast[idxToast]]];
        [_uiToast toastOut:0.5];
        
        modeSwitched = false;
    }
    
    if(_mapView.scrollEnabled)
    {
        return;
    }
    
    for (UITouch *touch in touches)
    {
        CGPoint pntTouch = [touch locationInView:_mapView];
        yTouch = pntTouch.y;
        
        // only allow one touch
        break;
    }
    
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event {
    
    if(_mapView.scrollEnabled)
    {
        return;
    }
    
    for (UITouch *touch in touches)
    {
        CGPoint pntTouch = [touch locationInView:_mapView];
        [self zoomMap:-(pntTouch.y - yTouch) / HEIGHT_SCREEN];
        yTouch = pntTouch.y;
        
        // only allow one touch
        break;
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
        if(diameter < WIDTH_SCREEN * 0.333)// || diameter > WIDTH_SCREEN * 0.8)
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
