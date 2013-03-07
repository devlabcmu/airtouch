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
#import "GLGestureRecognizer.h"
#import "GLGestureRecognizer+JSONTemplates.h"

#define DO_GESTURE_RECOGNITION  TRUE
#define SHOW_GESTURE            TRUE

@interface xacViewController : UIViewController
@property (strong, nonatomic) IBOutlet UIView *mainView;
@property (weak, nonatomic) IBOutlet MKMapView *mapView;
@property (weak, nonatomic) IBOutlet UIButton *btnConnect;
- (IBAction)connect:(id)sender;

@property xacNetworkStreaming* stream;
@property xacData* airData;

@property xacCurve* curveView;
@property (weak, nonatomic) IBOutlet UILabel *lbGestureRate;
@property (weak, nonatomic) IBOutlet UILabel *lbDataRate;
@property (weak, nonatomic) IBOutlet UILabel *lbGestureDetected;
@property (weak, nonatomic) IBOutlet UILabel *lbGestureDirection;

@property GLGestureRecognizer* recognizer;

@property int gesture;

@end
