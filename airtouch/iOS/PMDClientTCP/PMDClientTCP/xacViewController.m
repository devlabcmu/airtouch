//
//  xacViewController.m
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 1/5/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacViewController.h"
#define BASE_RADIUS 25
#define PORT 10000
#define IP_CMU "128.237.238.16"
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

- (void)viewDidLoad
{
    [super viewDidLoad];

    /*
        air data
     */
    _atp = [[xacAirTouchProfile alloc] init];
    _airData = [[xacData alloc] init:AIR_BUFFER_SIZE];
    
    /*	
        networking
     */
    _stream = nil;
    
    [NSTimer scheduledTimerWithTimeInterval:REQUEST_FREQUENCY
                                     target:self
                                   selector:@selector(sendSensorInfoToServer)
                                   userInfo:nil
                                    repeats:YES];
    
    /*
        a circle visualization of finger position
     */
    CGRect screenRect = [[UIScreen mainScreen] bounds];
    widthScreen = screenRect.size.width;
    heightScreen = screenRect.size.height;
    
    _circleView = [[UIView alloc] initWithFrame:CGRectMake(0,0,0,0)];
    _circleView.alpha = 0;
    _circleView.layer.cornerRadius = 0;
    _circleView.backgroundColor = [UIColor redColor];
    
    _curveView = [[xacCurve alloc] initWithFrame:CGRectMake(0, 0, widthScreen, heightScreen)];
    [_curveView setBackgroundColor: [UIColor colorWithRed:255 green:255 blue:255 alpha:0]];
    


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
        
        // visualize as circle
        [_mainView addSubview:_circleView];
        
        // visualize as trace
        [_mainView addSubview:_curveView];
        
        // controls
        [_mainView addSubview:_ctrlView];
        [_ctrlView setBackgroundColor:[UIColor colorWithRed:255 green:255 blue:255 alpha:0]];
        
//        [_curveView updateCurve:125 :150:175 :150:200:100];
//        [_curveView updateCurve:225 :50 :275 :75 :300 :200];

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
        _stream.isConnected = false;
        [_btnConnect setTitle:@"Connect" forState:UIControlStateNormal];
    }
}

long cntrTime = 0;

/*
    requesting sensor data from the server
 */
- (void)sendSensorInfoToServer {
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
        }
        cntrTime++;
        _curveView.alpha *= 0.99;

        NSString* strFPS = [NSString stringWithFormat:@"fps: %d", _stream.fps];
        [_lbFPS setText:strFPS];
    }
}

- (float) crop :(float) original :(float) lower :(float) higher
{
    if(original < lower) return lower;
    if(original > higher) return higher;
    return original;
}

- (void) touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
//    NSString* touchEventStr = @"touch began!";
//    NSLog();
//    for (UITouch *touch in touches)
//    {
//        CGPoint pntTouch = [touch locationInView:_viewMain];
//        NSString* touchEventStr = [NSString stringWithFormat:@"%f, %f, %f", touches.count * 1.0f, pntTouch.x, pntTouch.y];
//        [_stream sendStrToServer:touchEventStr];
//    }
    }

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
//    int tid = 0;
//    for (UITouch *touch in touches)
//    {
//        CGPoint pntTouch = [touch locationInView:_viewMain];
////        [_stp updateTouchPoints:tid :pntTouch.x :pntTouch.y];
////        NSString* touchEventStr = [NSString stringWithFormat:@"touch moved: %d finger(s) at (%f, %f)", touches.count, pntTouch.x, pntTouch.y];
//        NSString* touchEventStr = [NSString stringWithFormat:@"%f, %f, %f", touches.count * 1.0f, pntTouch.x, pntTouch.y];
//        [_stream sendStrToServer:touchEventStr];
//        tid++;
//    }
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
//    NSString* touchEventStr = @"touch ended!";
//    NSLog();
//    [_stream sendStrToServer:touchEventStr];
    
    _curveView.alpha = 1.0;
    [_curveView setNeedsDisplay];

}
@end
