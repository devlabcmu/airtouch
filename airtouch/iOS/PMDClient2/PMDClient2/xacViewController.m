//
//  xacViewController.m
//  PMDclient
//
//  Created by Xiang 'Anthony' Chen on 12/3/12.
//  Copyright (c) 2012 hotnAny. All rights reserved.
//

#import "xacViewController.h"

@interface xacViewController ()

@end

@implementation xacViewController

NSString* ipAddr;
int port;
bool isConnected = false;

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
    _udpSocket = [[AsyncUdpSocket alloc] initWithDelegate:self];
    // change the ip address
    ipAddr = @"128.237.226.252";
    port = 21027;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


- (IBAction)connectToServer:(id)sender {

    if(!isConnected)
    {
        NSError *error = nil;
        if (![_udpSocket bindToPort:port error:&error])
        {
            NSLog(@"Error binding: %@", error);
            return;
        }
        
        [_udpSocket receiveWithTimeout:-1 tag:0];
        
        isConnected = true;
    }
    
    if(isConnected)
    {
    //    NSData *data = [[NSData alloc] init];
        NSString *msg = @"connect me!";
        NSData *data = [msg dataUsingEncoding:NSUTF8StringEncoding];
        [_udpSocket sendData:data toHost:ipAddr port:port withTimeout:-1 tag:_tag];
    }
}

- (BOOL)onUdpSocket:(AsyncUdpSocket *)sock
     didReceiveData:(NSData *)data
            withTag:(long)tag
           fromHost:(NSString *)host
               port:(UInt16)port
{
	NSString *msg = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
	if (msg)
	{
        [_lbInfo setText:msg];
	}
	else
	{
        NSLog(@"Error converting received data into UTF-8 String");
	}
    
    
	return YES;
}
@end
