//
//  xacNetworkStreaming.m
//  PMDClientTCP
//
//  Created by Xiang 'Anthony' Chen on 11/20/12.
//  Copyright (c) 2012 hotnAny. All rights reserved.
//

#import "xacNetworkStreaming.h"
#include <stdint.h>

@implementation xacNetworkStreaming

NSMutableData *data;

NSInputStream *iStream;
NSOutputStream *oStream;
NSString *msgFromServer;

CFReadStreamRef readStream = NULL;
CFWriteStreamRef writeStream = NULL;

- (id) init: (xacAirTouchProfile*) atp
{
    self = [super init];
    _atp = atp;
    _isConnected = false;
    return self;
}


// establish connection
-(void) connectToServer{
    [self connectToServer:_ipAddr portNo:_port];
}


// establish connection
-(void) connectToServer:(NSString *) urlStr portNo: (uint) portNo {
    CFStreamCreatePairWithSocketToHost(kCFAllocatorDefault,
                                       (__bridge CFStringRef) urlStr,
                                       portNo,
                                       &readStream,
                                       &writeStream);
    
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


// sending NSData to server
-(void) sendToServer:(NSData *) msg
{
//    NSLog(@"sent to server");
    if(msg != nil)
    {
        uint32_t length = (uint32_t)htonl([msg length]);
        [oStream write:[msg bytes] maxLength:[msg length]];
    }
}


// sending string to server
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


// handling received data
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

                    int xInt = [self getFloatInByteArray:buf :0];
                    float xFloat = [self intBitsToFloat:xInt];
                    
                    int yInt = [self getFloatInByteArray:buf :4];
                    float yFloat = [self intBitsToFloat:yInt];
                    
                    int zInt = [self getFloatInByteArray:buf :8];
                    float zFloat = [self intBitsToFloat:zInt];
                    
                    [_atp updateRawData:xFloat :yFloat :zFloat];
                    
//                    NSString *dataStr = [NSString stringWithFormat:@"%f, %f, %f", _atp.caliX, _atp.caliY, _atp.caliZ];
//                    NSLog(dataStr);
                }
                else
                {
                    NSLog(@"No data.");
                }                
            }
            break;
    }
    
}


// get the byets at different positions in an array
- (int) getFloatInByteArray:(Byte[])bytes: (int) startOffset
{
    // 0xF is 4 bits, 0xFF is 8 bits
    int asInt = (bytes[startOffset + 0] & 0xFF)
    | ((bytes[startOffset + 1] & 0xFF) << 8)
    | ((bytes[startOffset + 2] & 0xFF) << 16)
    | ((bytes[startOffset + 3] & 0xFF) << 24);
    return asInt;
}


// returns the float value corresponding to a given bit represention.of a scalar int value or vector of int values
- (float) intBitsToFloat: (int) bits
{
    int sign     = ((bits & 0x80000000) == 0) ? 1 : -1;
    int exponent = ((bits & 0x7f800000) >> 23);
    int mantissa =  (bits & 0x007fffff);
    
    mantissa |= 0x00800000;
    // Calculate the result:
    float f = (float)(sign * mantissa * pow(2, exponent-150));
    return f;
}

@end
