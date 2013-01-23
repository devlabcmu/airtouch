//
//  xacViewController.m
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 1/5/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacViewController.h"

@interface xacViewController ()

@end

@implementation xacViewController

NSString *ipAddr = NULL;
int port = 10000;
float intrvl = 0.01;

//NSString *ipLoc874 = @"874";
NSString *ipAddr874 = @"192.168.8.112";
NSString *ipAddrCMU = @"128.237.244.76";
//NSString

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
    
    _atp = [[xacAirTouchProfile alloc] init];
    _stp = [[xacScreenTouchProfile alloc] init];
    
    _stream = nil;
//    ipAddr = ipAddr874;
    ipAddr = ipAddrCMU;
    
    [NSTimer scheduledTimerWithTimeInterval:intrvl
                                     target:self
                                   selector:@selector(sendSensorInfoToServer)
                                   userInfo:nil
                                    repeats:YES];
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
        _stream.ipAddr = ipAddr;
        _stream.port = port;
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
    
        // changing the button's position to vis the received data 
//        CGRect screenRect = [[UIScreen mainScreen] bounds];
//        float screenWidth = screenRect.size.width;
//        float screenHeight = screenRect.size.height;
        
//        CGSize size = _btnConnect.frame.size;
//        float xBtn = screenWidth * _atp.caliX * 2;
//        float yBtn = screenHeight * _atp.caliZ;
//        _btnConnect.frame = CGRectMake(xBtn, yBtn, size.width, size.height);
    }
}

- (void) touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    NSString* touchEventStr = @"touch began!";
//    NSLog();
    [_stream sendStrToServer:touchEventStr];
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event {
    int tid = 0;
    for (UITouch *touch in touches)
    {
        CGPoint pntTouch = [touch locationInView:_viewMain];
        [_stp updateTouchPoints:tid :pntTouch.x :pntTouch.y];
        NSString* touchEventStr = [NSString stringWithFormat:@"touch moved: %d finger(s) at (%f, %f)", touches.count, pntTouch.x, pntTouch.y];
//        NSLog(@"%@", );
        [_stream sendStrToServer:touchEventStr];
        tid++;
    }
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event {
    NSString* touchEventStr = @"touch ended!";
//    NSLog();
    [_stream sendStrToServer:touchEventStr];
}
@end
