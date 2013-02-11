//
//  xacNetworkStreaming.m
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 11/20/12.
//  Copyright (c) 2012 hotnAny. All rights reserved.
//

#import "xacNetworkStreaming.h"
#include <stdint.h>

#define TCP_NODELAY     0x0001
#define PMDIMAGESIZE    19800

@implementation xacNetworkStreaming

NSMutableData *data;

NSInputStream *iStream;
NSOutputStream *oStream;
NSString *msgFromServer;

CFReadStreamRef readStream = NULL;
CFWriteStreamRef writeStream = NULL;

//
// out of date
//
//typedef struct {
//	float finger1X;
//	float finger1Y;
//	float finger1Z;
//    
//	float finger2X;
//	float finger2Y;
//	float finger2Z;
//	float buffer[PMDIMAGESIZE];
//} PMDData;

typedef struct {
	int id; // 4 bytes
	float x; // 4 bytes
	float y; // 4 bytes
	float z; // 4 bytes
} PMDFinger;

typedef struct {
	PMDFinger fingers[2];
	float buffer[PMDIMAGESIZE];
} PMDData;

float t;
long cntFrames;

- (id) init: (xacData*) data
{
    self = [super init];
//    _atp = atp;
    _airData = data;
    _isConnected = false;
    _fps = 0;
    t = CACurrentMediaTime();
    cntFrames = 0;
    return self;
}


// establish connection
-(void) connectToServer{
//    [self connectToServer:"128.237.250.21" portNo:_port];
    [self connectToServer:_ipAddr portNo:_port];
}


// establish connection
-(void) connectToServer:(char *) urlStr portNo: (uint) portNo {
    
    /*
     latency-aware way
     */
    
    int sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    
    int opt = 1;
    setsockopt(sock, IPPROTO_TCP, TCP_NODELAY, &opt, sizeof(opt));
    
    struct sockaddr_in addr = {0};
    bzero((char *) &addr, sizeof(addr));
	addr.sin_port = htons(portNo);
    inet_pton(AF_INET, urlStr, &addr.sin_addr);
    
    if (connect(sock, (struct sockaddr *) &addr, sizeof(addr)) == 0)
	{
        CFStreamCreatePairWithSocket(kCFAllocatorDefault,
                                     sock,
                                     (CFReadStreamRef *)(&readStream),
                                     (CFWriteStreamRef *)(&writeStream));
    }
    
    /*
        bandwidth-sensitive way
     */
//    NSString* urlNSStr = [[NSString alloc] initWithUTF8String:urlStr];
//    CFStreamCreatePairWithSocketToHost(kCFAllocatorDefault,
//                                       (__bridge CFStringRef) urlNSStr,
//                                       portNo,
//                                       &readStream,
//                                       &writeStream);
    
    if (readStream && writeStream) {
        CFReadStreamSetProperty(readStream,
                                kCFStreamPropertyShouldCloseNativeSocket,
                                kCFBooleanTrue);
        CFWriteStreamSetProperty(writeStream,
                                 kCFStreamPropertyShouldCloseNativeSocket,
                                 kCFBooleanTrue);
        
        iStream = (__bridge NSInputStream *)readStream;
        [iStream setDelegate:self];
        [iStream scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
        [iStream open];
        
        oStream = (__bridge NSOutputStream *)writeStream;
        [oStream setDelegate:self];
        [oStream scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
        [oStream open];
    }
}


/*
    sending NSData to server
 */
-(void) sendToServer:(NSData *) msg
{
//    NSLog(@"sent to server");
    if(msg != nil)
    {
//        uint32_t length = (uint32_t)htonl([msg length]);
        [oStream write:[msg bytes] maxLength:[msg length]];
    }
}


/*
    sending string to server
 */
-(void) sendStrToServer:(NSString *) msg
{
    if(msg != nil)
    {
        NSData *msgData = [msg dataUsingEncoding:NSUTF8StringEncoding];
        int bytesWritten = [oStream write:[msgData bytes] maxLength:[msgData length]];
        if(bytesWritten < 0)
        {
            [self connectToServer];
        }
//        NSLog([NSString stringWithFormat:@"bytes written: %d", bytesWritten]);
    }
}


/*
    handling received data
 */
- (void)stream:(NSStream *)stream handleEvent:(NSStreamEvent)eventCode
{
    switch(eventCode) {
        case NSStreamEventHasBytesAvailable:
            if(stream == iStream)
            {
                Byte buf[1024];
                unsigned int len = 0;
                len = [(NSInputStream *)stream read:buf maxLength:1024];

                if(len) {
                    
                    PMDData* tmpData = (PMDData*)buf;

                    int idData = tmpData->fingers[0].id;
                    float xFloat = tmpData->fingers[0].x;
                    float yFloat = tmpData->fingers[0].y;
                    float zFloat = tmpData->fingers[0].z;
                    
//                    [_atp updateRawData:xFloat :yFloat :zFloat];
                    [_airData update:xFloat :yFloat :zFloat];
                    
                    // debug routines
//                    NSString *dataStr = [NSString stringWithFormat:@"%f, %f, %f", _atp.caliX, _atp.caliY, _atp.caliZ];
//                    NSString *dataStr = [NSString stringWithFormat:@"%f, %f, %f, %f", _atp.rawXMin, xFloat, _atp.rawXMax, _atp.caliX];
                    NSString *dataStr = [NSString stringWithFormat:@"%d, %f, %f, %f", idData, xFloat, yFloat, zFloat];
                    NSLog(@"%@", dataStr);
                    
                    double now = CACurrentMediaTime();
                    cntFrames++;
                    if(now - t > 1)
                    {
                        _fps = cntFrames / (now - t);
//                        NSLog(@"%@", [NSString stringWithFormat:@"%d", _fps]);
                        t = now;
                        cntFrames = 0;
                    }
                }
                else
                {
                    _fps = 0;
                    NSLog(@"No data.");
                }                
            }
            break;
    }
    
}

@end

/*
 honorable mentioned code
 */

// get the byets at different positions in an array
//- (int) getFloatInByteArray:(Byte[])bytes: (int) startOffset
//{
//    // 0xF is 4 bits, 0xFF is 8 bits
//    int asInt = (bytes[startOffset + 0] & 0xFF)
//    | ((bytes[startOffset + 1] & 0xFF) << 8)
//    | ((bytes[startOffset + 2] & 0xFF) << 16)
//    | ((bytes[startOffset + 3] & 0xFF) << 24);
//    return asInt;
//}
//
//
//// returns the float value corresponding to a given bit represention.of a scalar int value or vector of int values
//- (float) intBitsToFloat: (int) bits
//{
//    int sign     = ((bits & 0x80000000) == 0) ? 1 : -1;
//    int exponent = ((bits & 0x7f800000) >> 23);
//    int mantissa =  (bits & 0x007fffff);
//    
//    mantissa |= 0x00800000;
//    // Calculate the result:
//    float f = (float)(sign * mantissa * pow(2, exponent-150));
//    return f;
//}
