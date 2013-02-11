//
//  xacNetworkStreaming.h
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 11/20/12.
//  Copyright (c) 2012 hotnAny. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <QuartzCore/QuartzCore.h>
#import "xacAirTouchProfile.h"
#import "xacData.h"

#import <sys/types.h>
#import <sys/socket.h>
#import <netinet/in.h>
#import <arpa/inet.h>
#import <poll.h>
#import <netdb.h>
#import <sys/types.h>
#import <sys/sysctl.h>
#include <time.h>

@interface xacNetworkStreaming : NSObject

@property bool isConnected;
@property xacAirTouchProfile* atp;
@property xacData* airData;
@property char* ipAddr;
@property int port;
@property int fps;

- (id) init:(xacData*) data;
- (void) connectToServer;
- (void) connectToServer:(char *) urlStr portNo: (uint) portNo;
- (void) sendToServer:(NSData *) msg;
- (void) sendStrToServer:(NSString *) msg;

@end
