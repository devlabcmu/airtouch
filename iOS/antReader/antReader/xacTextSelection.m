//
//  xacTextSelection.m
//  antReader
//
//  Created by Xiang 'Anthony' Chen on 3/4/13.
//  Copyright (c) 2013 hotnAny. All rights reserved.
//

#import "xacTextSelection.h"



@implementation xacTextSelection

float xAir;
float yAir;
float zAir;

int counterTextSel = TEXT_SEL_TIME_OUT;

- (id) init
{
    self = [super init];
    _textSelState = TS_DEFAULT;
    xAir = yAir = zAir = 0;
    return self;
}

// z is height
- (void) update :(float) x :(float) y :(float)z
{
    xAir = x;
    yAir = y;
    zAir = z;
    
    if(counterTextSel >= TEXT_SEL_TIME_OUT)
    {
        _textSelState = TS_DEFAULT;
        if(counterTextSel == TEXT_SEL_TIME_OUT)
        {
            NSLog(@"time out...");
//            [_textView selectAll:_textView];
//            [_textView setSelectedTextRange:nil];
            counterTextSel++;
        }
        return;
    }
    else    counterTextSel++;
    
    float height = z;
    enum TextSelectionState tmpState = _textSelState;
    switch (_textSelState) {
        case TS_DEFAULT:
            if(height < THRES_LOW) tmpState = TS_LOW;
            if(_textSelState != tmpState) NSLog(@"LOW");
            break;
        case TS_LOW:
            if(height > THRES_HIGH_UP) tmpState = TS_HIGH_UP;
            if(_textSelState != tmpState)
            {   NSLog(@"HIGH_UP"); _selectionEnabled = true;    }
            break;
        case TS_HIGH_UP:
//            [self dynmSelect];
            if(height < MIN_HEIGHT * 1.5) tmpState = TS_LOW_AGAIN;
            // maybe add a time out
            if(_textSelState != tmpState) NSLog(@"LOW_AGAIN");
            break;
        case TS_LOW_AGAIN:
//            _selectionEnabled = true;
            break;
        default:
            break;
    }
    
    _textSelState = tmpState;
    
}

- (void) dynmSelect
{
    float xEnd = _pntTouchSelStart.x + (xAir - _pntAirSelStart.x);
    float yEnd = _pntTouchSelStart.y + (yAir - _pntAirSelStart.y) * 0.75;
    xEnd = MIN(xEnd, WIDTH_SCREEN);
//    NSLog(@"%f, %f", xEnd, yEnd);
    [self selectText:CGPointMake(xEnd, yEnd)];
}

- (void) initSelection :(UITextView*) textView
{
    _textView = textView;
    [_textView selectAll:_textView];
    [_textView setSelectedTextRange:nil];
    
}

- (void) startSelection: (CGPoint) pntTouch
{
    _selectionEnabled = false;
    [_textView selectAll:_textView];
    [_textView setSelectedTextRange:nil];
    _pntTouchSelStart = pntTouch;
    _headSelection = [_textView closestPositionToPoint:_pntTouchSelStart];
            _textSelState = TS_DEFAULT;
    _pntAirSelStart = CGPointMake(xAir, yAir);
    counterTextSel = 0;
    
}

- (void) finishSelection: (CGPoint) pntTouch
{
    if(_selectionEnabled)
    {
        [self selectText:pntTouch];
        NSLog(@"selection confirmed");

    }
    else
    {
        NSLog(@"%d", counterTextSel);
    }
    
    _textSelState = TS_DEFAULT;
    counterTextSel = 0;
}

- (void) selectText: (CGPoint) pntTouch
{
    UITextPosition* endSelection = [_textView closestPositionToPoint:pntTouch];
    UITextRange* rangeSelection = [_textView textRangeFromPosition: _headSelection toPosition:endSelection];
    
//    int posStart = [_textView offsetFromPosition:_textView.beginningOfDocument
//                                      toPosition:rangeSelection.start];
//    int posEnd = [_textView offsetFromPosition:_textView.beginningOfDocument
//                                    toPosition:rangeSelection.end];
    
    //    NSLog(@"%d, %d", posStart, posEnd);
    [_textView setSelectedTextRange:rangeSelection];
    //        NSLog(@"%@", [_textView textInRange:rangeSelection]);

}

- (BOOL) isTimeOut
{
//    UITextRange* selectedRange = [_textView selectedTextRange];
//    return !selectedRange.empty;
////    return _textView se
    
    return counterTextSel >= TEXT_SEL_TIME_OUT;
}

@end
