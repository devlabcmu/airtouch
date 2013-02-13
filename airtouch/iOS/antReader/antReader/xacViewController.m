//
//  xacViewController.m
//  antRange
//
//  Created by Xiang 'Anthony' Chen on 2/11/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacViewController.h"
#define PORT 10000
#define IP_CMU "128.237.238.16"
#define IP_874 "192.168.8.112"
#define REQUEST_FREQUENCY 0.01
#define AIR_BUFFER_SIZE 128
#define NUM_POINTS 3
//#define MAX_HEIGHT 0.06
//#define THRS_WIDTH_RATIO 0.75
//#define THRS_HEIGHT_RATIO 0.75
//#define FADE_TIME_OUT 100

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
    
    CGRect screenRect = [[UIScreen mainScreen] bounds];
    widthScreen = screenRect.size.width;
    heightScreen = screenRect.size.height;
    
    _uiFade = [[xacUIFade alloc] init];
    [_uiFade addUICtrl:_btnLibrary];
    [_uiFade addUICtrl:_btnSettings];
    [_uiFade addUICtrl:_sldChapters];
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
    //    NSLog(@"sensor info sent");
    
    if(_stream.isConnected)
    {
        [_stream sendStrToServer:@"f"];
                
        xacVector* airCoord = _airData.vecRaw;
        float xCenter = [self crop: airCoord.rawX * WIDTH_SCREEN: 0: WIDTH_SCREEN];
        float yCenter = [self crop: airCoord.rawZ * HEIGHT_SCREEN: 0: HEIGHT_SCREEN];
        float height = [self crop:airCoord.rawY :0 :MAX_HEIGHT];
        
        [_uiFade update:xCenter :yCenter :height];
    }
}

- (float) crop :(float) original :(float) lower :(float) higher
{
    if(original < lower) return lower;
    if(original > higher) return higher;
    return original;
}

@end
