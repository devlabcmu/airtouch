//
//  xacViewController.h
//  antMap
//
//  Created by Xiang 'Anthony' Chen on 3/6/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <MapKit/MapKit.h>
#import "xacNetworkStreaming.h"
#import "xacConstants.h"
#import "xacCurve.h"
#import "xacUIToast.h"
#import "GLGestureRecognizer.h"
#import "GLGestureRecognizer+JSONTemplates.h"
#import "xacUIShadow.h"

#define REQUEST_FREQUENCY   50
#define UI_UPDATE_FREQUENCY 100
#define PORT                10000
#define IP_CMU              "128.237.126.216"

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

#define DO_GESTURE_RECOGNITION  TRUE
#define DO_HIGH_MODE_SWITCHING  TRUE
#define SHOW_GESTURE            TRUE
#define SHOW_FINGER_SHADOW      TRUE

#define MODE_SWITCH_TIME_OUT 50

#define THRES_HIGH_UP   0.040
#define THRES_LOW       0.030

#define DOT_PRODUCT_EPS 0.1

@interface xacViewController : UIViewController
@property (strong, nonatomic) IBOutlet UIView *mainView;
@property (weak, nonatomic) IBOutlet MKMapView *mapView;
@property (weak, nonatomic) IBOutlet UIButton *btnConnect;


enum ModeSwitchingState
{
    MS_DEFAULT,
    MS_LOW,
    MS_HIGH_UP,
    MS_LOW_AGAIN
};
@property enum ModeSwitchingState modeSwtichingState;

enum CycleZoomState
{
    CZ_DEFAULT,
    CZ_ACTIVATED,
    CZ_ZOOMING
};
@property enum CycleZoomState cycleZoomState;

@property xacNetworkStreaming* stream;
@property xacData* airData;

@property xacCurve* curveView;
@property (weak, nonatomic) IBOutlet UILabel *lbGestureRate;
@property (weak, nonatomic) IBOutlet UILabel *lbDataRate;
@property (weak, nonatomic) IBOutlet UILabel *lbGestureDetected;
@property (weak, nonatomic) IBOutlet UILabel *lbGestureDirection;

@property GLGestureRecognizer* recognizer;

@property int gesture;

@property xacUIToast* uiToast;
@property xacUIShadow* uiShadow;

- (IBAction)connect:(id)sender;
@end
