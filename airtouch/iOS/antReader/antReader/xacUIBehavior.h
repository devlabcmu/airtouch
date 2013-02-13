//
//  xacUIBehavior.h
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 2/12/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface xacUIBehavior : NSObject

- (id) init;
- (void) update :(float) val;
- (void) update :(float) x :(float) y :(float)z;

@end
