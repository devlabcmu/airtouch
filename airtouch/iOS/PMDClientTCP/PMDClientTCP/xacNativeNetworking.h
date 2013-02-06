//
//  xacNativeNetworking.h
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 2/5/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <Foundation/Foundation.h>

#import <sys/types.h>
#import <sys/socket.h>
#import <netinet/in.h>
#import <sys/types.h>
#import <arpa/inet.h>
#import <poll.h>
#import <netdb.h>
#import <sys/types.h>
#import <sys/sysctl.h>

@protocol OutputStream, InputStream;

@interface xacNativeNetworking : NSObject

@property CFWriteStreamRef*	sendStream;
@property CFReadStreamRef*	receiveStream;

@property NSInputStream *iStream;
@property NSOutputStream *oStream;

//@property id<OutputStream>	sendStream;
//@property id<InputStream>	receiveStream;

- (void) initConnection: (char*)ipAddr :(int) port;

@end
