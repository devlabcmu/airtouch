//
//  xacViewController.h
//  PMDclient
//
//  Created by Xiang 'Anthony' Chen on 12/3/12.
//  Copyright (c) 2012 hotnAny. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "AsyncUdpSocket.h"

@interface xacViewController : UIViewController

- (IBAction)connectToServer:(id)sender;
@property (weak, nonatomic) IBOutlet UILabel *lbInfo;
@property AsyncUdpSocket *udpSocket;
@property long tag;

@end
