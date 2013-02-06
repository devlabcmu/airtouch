//
//  xacViewController.m
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 1/5/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacViewController.h"
#define BASE_RADIUS 50
#define PORT 10000
#define IP_CMU "128.237.250.21"
#define IP_874 "192.168.8.112"

@interface xacViewController ()

@end

@implementation xacViewController

//NSString *ipAddr = NULL;
//int port = 10000;
float intrvl = 0.01;

//NSString *ipAddr874 = @"192.168.8.112";
//NSString *ipAddrCMU = @"128.237.250.21";

int widthScreen = -1;
int heightScreen = -1;

- (void)viewDidLoad
{
    [super viewDidLoad];

    _atp = [[xacAirTouchProfile alloc] init];
//    _stp = [[xacScreenTouchProfile alloc] init];
    
    _stream = nil;
    
    [NSTimer scheduledTimerWithTimeInterval:intrvl
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
    
    [_mainView addSubview:_circleView];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

// connect/disconnect to the server
- (IBAction)connect:(id)sender {
    if(_stream == nil)
    {
        _stream = [[xacNetworkStreaming alloc] init: _atp];
        _stream.ipAddr = IP_CMU;
        _stream.port = PORT;
    }
    
    if(!_stream.isConnected)
    {
        [_stream connectToServer];
        _stream.isConnected = true;
        [_btnConnect setTitle:@"Disconnect" forState:UIControlStateNormal];
    }
    else
    {
        _stream.isConnected = false;
        [_btnConnect setTitle:@"Connect" forState:UIControlStateNormal];
    }
}

- (void)sendSensorInfoToServer {
    //    NSLog(@"sensor info sent");
    
    if(_stream.isConnected)
    {
        [_stream sendStrToServer:@"finger"];
        
        float xCenter = _stream.atp.caliX * widthScreen;
        float yCenter = _stream.atp.caliZ * heightScreen;
        float radius = (1 + _stream.atp.caliY) * BASE_RADIUS;
        
        _circleView.alpha = 0.75 * (1 - _stream.atp.caliY) + 0.25;
        _circleView.frame = CGRectMake(0, 0, 2 * radius, 2 * radius);
        _circleView.layer.cornerRadius = radius;
        [_circleView setCenter:CGPointMake(xCenter, yCenter)];

        NSString* strFPS = [NSString stringWithFormat:@"fps: %d", _stream.fps];
        [_lbFPS setText:strFPS];
    }
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
}
@end
