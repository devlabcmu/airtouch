//
//  xacNetworkStreaming.h
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 11/20/12.
//  Copyright (c) 2012 hotnAny. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "xacAirTouchProfile.h"

@interface xacNetworkStreaming : NSObject

@property bool isConnected;
@property xacAirTouchProfile* atp;
@property NSString* ipAddr;
@property int port;

- (id) init:(xacAirTouchProfile*) atp;
- (void) connectToServer;
- (void) connectToServer:(NSString *) urlStr portNo: (uint) portNo;
- (void) sendToServer:(NSData *) msg;
- (void) sendStrToServer:(NSString *) msg;

@end
