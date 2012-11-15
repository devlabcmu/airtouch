#pragma once


#include "ofMain.h"
#include "ofxiPhone.h"
#include "ofxiPhoneExtras.h"
#include "Ball.h"

class testApp : public ofxiPhoneApp{
	
    
    
public:
    
    typedef enum {
        ToolClassFinger=0,
        ToolClassMouse,
        ToolClassPen,
        ToolClassRuler,
        ToolClassCount
    } ToolClass;
    
    string toolClassStrs[4];
    string touchTypeStrs[5];
    
    testApp()
    {
        toolClassStrs[0] = "Finger";
        toolClassStrs[1] = "Mouse";
        toolClassStrs[2] = "Pen";
        toolClassStrs[3] = "Ruler";
        
        touchTypeStrs[0] = "TOUCH_DOWN";
        touchTypeStrs[1] = "TOUCH_UP";
        touchTypeStrs[2] = "TOUCH_MOVE";
        touchTypeStrs[3] = "TOUCH_DOUBLETAP";
        touchTypeStrs[4] = "TOUCH_CANCEL";
        
        currentClass = ToolClassFinger;
        numBallsDragging = 0;
        
    }
    
    
    
    void setup();
    void update();
    void draw();
    void exit();
    
    void touchDown(ofTouchEventArgs & touch);
    void touchMoved(ofTouchEventArgs & touch);
    void touchUp(ofTouchEventArgs & touch);
    void touchDoubleTap(ofTouchEventArgs & touch);
    void touchCancelled(ofTouchEventArgs & touch);
	
    void lostFocus();
    void gotFocus();
    void gotMemoryWarning();
    void deviceOrientationChanged(int newOrientation);
	
    void gotMessage(ofMessage msg);
    
    void writeTouchUpdated(ofTouchEventArgs & touch);
    void newTouchDir();
    
    
    ofImage arrow;
    vector<Ball> balls;
    int numBallsDragging;
    
    
    // File I/0
    void updateDocsDir()
    {
        NSArray *dirPaths;
        dirPaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,
                                                       NSUserDomainMask, YES);
        docsDir = [dirPaths objectAtIndex:0];
    }
    NSFileManager *filemgr;
    NSString *docsDir;
    string touch_file_path;
    unsigned int start_time;
    
    ToolClass currentClass;
    void nextClass()
    {
        currentClass++;
        if(currentClass >= ToolClassCount)
            currentClass = ToolClassFinger;
    }
};
