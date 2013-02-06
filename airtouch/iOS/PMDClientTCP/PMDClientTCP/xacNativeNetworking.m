//
//  xacNativeNetworking.m
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 2/5/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacNativeNetworking.h"
#define TCP_NODELAY         0x0001

@implementation xacNativeNetworking

- (CFSocketNativeHandle)socket
{
	CFSocketNativeHandle native;
	CFDataRef nativeProp = CFReadStreamCopyProperty ((CFReadStreamRef)_receiveStream, kCFStreamPropertySocketNativeHandle);
	if (nativeProp == NULL)
	{
		return -1;
	}
	CFDataGetBytes (nativeProp, CFRangeMake(0, CFDataGetLength(nativeProp)), (UInt8 *)&native);
	CFRelease (nativeProp);
	return native;
}

- (void) initConnection: (char*)ipAddr :(int) port
{
    int sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    
    int opt = 1;
    setsockopt(sock, IPPROTO_TCP, TCP_NODELAY, &opt, sizeof(opt));
    
    struct sockaddr_in addr = {0};
    bzero((char *) &addr, sizeof(addr));
//	addr.sin_family = PF_INET;
	addr.sin_port = htons(port);
    inet_pton(AF_INET, ipAddr, &addr.sin_addr);
    
    if (connect(sock, (struct sockaddr *) &addr, sizeof(addr)) == 0)
	{
		CFStreamCreatePairWithSocket(kCFAllocatorDefault,
									 sock,
                                       (CFReadStreamRef *)(&_receiveStream),
									 (CFWriteStreamRef *)(&_sendStream));
//									 _receiveStream,
//									 _sendStream);		// "Ownership follows the Create Rule."
        
		// CFStreamCreatePairWithSocket does not close the socket by default
		CFReadStreamSetProperty((CFReadStreamRef)_receiveStream, kCFStreamPropertyShouldCloseNativeSocket, kCFBooleanTrue);
		CFWriteStreamSetProperty((CFWriteStreamRef)_sendStream, kCFStreamPropertyShouldCloseNativeSocket, kCFBooleanTrue);
	}
    
    if(_receiveStream && _sendStream)
    {
        _oStream = objc_unretainedObject(_sendStream);
        _iStream = objc_unretainedObject(_receiveStream);
    }
}

- (void)stream:(NSStream *)stream handleEvent:(NSStreamEvent)eventCode
{
//    NSStream* cfSendStream = objc_unretainedObject(_sendStream);
//    NSStream* cfReceiveStream = objc_unretainedObject(_receiveStream);
    
//    CFReadStreamRef* cfSendStream = stream;
    
//    if (stream == cfSendStream) {
//		[self handleSendStreamEvent:eventCode];
//	} else if (stream == cfReceiveStream) {
//		[self handleReceiveStreamEvent:eventCode];
//	}
}

- (void)handleSendStreamEvent:(NSStreamEvent)theEvent
{
    
}

- (void)handleReceiveStreamEvent:(NSStreamEvent)theEvent
{
    
}

-(void) sendStrToServer:(NSString *) msg
{
    if(msg != nil)
    {
        NSData *msgData = [msg dataUsingEncoding:NSUTF8StringEncoding];
        NSOutputStream* cfSendStream = objc_unretainedObject(_sendStream);
//        int bytesWritten = [_sendStream
        int bytesWritten = [cfSendStream write:[msgData bytes] maxLength:[msgData length]];
//        if(bytesWritten < 0)
//        {
//            [self connectToServer];
//        }
        //        NSLog([NSString stringWithFormat:@"bytes written: %d", bytesWritten]);
    }
}

@end
