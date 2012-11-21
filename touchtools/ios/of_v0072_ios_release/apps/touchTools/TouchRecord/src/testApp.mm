#include "testApp.h"
#define TOUCH_AREA_MULTIPLIER 5.2
#define TOUCH_INVALID_AREA 70


//--------------------------------------------------------------
void testApp::setup(){
    start_time = ofGetSystemTime();
	ofBackground(225, 225, 225);
	ofSetCircleResolution(80);
    ofSetLogLevel(OF_LOG_VERBOSE);
	
	balls.assign(20, Ball());
	
	// initialize all of the Ball particles
	for(int i=0; i<balls.size(); i++){
		balls[i].init(i);
	}
    
    // init buttons
    int w = ofGetWidth();
    int btnW = 80;
    int btnH = 30;
    int btnP = 20;
    nextBtn.init("next", w - (btnW + btnP), 10, btnW, btnH, ofColor(0,0,255));
    prevBtn.init("prev", w - 2 * (btnW + btnP), 10, btnW, btnH, ofColor(0,0,255));
    delBtn.init("rm all", w - 3 * (btnW + btnP), 10, btnW, btnH, ofColor(255,0,0));
    delLastBtn.init("rm last", w - 4 * (btnW + btnP), 10, btnW, btnH, ofColor(255,0,0));
    recBtn.init("record", w - 5 * (btnW + btnP), 10, btnW, btnH, ofColor(255,0,0));
    // file io setup
    NSArray *dirPaths;
    filemgr =[NSFileManager defaultManager];
    
    updateDocsDir();
    
    ostringstream oss;
    oss << [docsDir UTF8String] << "/" ;

    ofSetDataPathRoot(oss.str());
}


void testApp::newTouchDir()
{
    // date-time_class
    bool success;
    
    string cls = toolClassStrs[(int)currentClass];
    ostringstream oss;
    // eg: %Y-%m-%d-%H-%M-%S-%i ( 2011-01-15-18-29-35-299 )
    oss<< ofGetTimestampString("%Y%m%d-%h%M%S%i") << "_" << cls;
    string path = oss.str();
    if(!ofDirectory::doesDirectoryExist(path))
    {
        ofLog(OF_LOG_VERBOSE, "making dir %s...", path.c_str());
        success = ofDirectory::createDirectory(path);
        if(!success)
            return;
    }
    
    // make touches.csv file
    touch_file_path= ofFilePath::join(path, "touches.csv");
    
    ofFile tf = ofFile(touch_file_path, ofFile::Append, false);
    if(!tf.exists())
    {
        ofLog(OF_LOG_VERBOSE, "making file %s...", tf.getAbsolutePath().c_str());
        success = tf.create();
        if(!success)
            return;
    }
    
    
    tf << "type,id,x,y,major_axis,sys_time,time_start" << endl;
    ofLog(OF_LOG_VERBOSE, "succesfully initialized file %s", tf.getAbsolutePath().c_str());
    
}

void testApp::writeTouchUpdated(ofTouchEventArgs & touch)
{
    // ofLog(OF_LOG_VERBOSE, "tfp is %s", touch_file_path.c_str());
    ofFile tf = ofFile(touch_file_path, ofFile::Append, false);
    if(!tf.exists())
    {
        ofLog(OF_LOG_VERBOSE, "touch file doesn't exist, not writing update...");
        return;
    }
    uint curtime = ofGetSystemTime();
    uint elapsed = curtime - start_time;
    tf << touchTypeStrs[touch.type] << "," << touch.id << "," << touch.x << "," << touch.y << ",";
    tf << touch.majoraxis << "," << curtime << "," << elapsed << endl;
    tf.close();
    
}

//--------------------------------------------------------------
void testApp::update() {
    bool hit = false;
	for(int i=0; i < balls.size(); i++){
		balls[i].update();
        if(balls[i].bTouchDown)
        {
            if(!hit && nextBtn.hitTest(balls[i].pos))
            {
                // go to next class
                nextClass();
                hit = true;
                balls[i].bTouchDown = false;
            }
            if(!hit && prevBtn.hitTest(balls[i].pos))
            {
                // go to prev class
                prevClass();
                hit = true;
                balls[i].bTouchDown = false;
            }
            if(!hit && delBtn.hitTest(balls[i].pos))
            {
                deleteAll();
                hit = true;
                balls[i].bTouchDown = false;
            }
            if(!hit && delLastBtn.hitTest(balls[i].pos))
            {
                deleteMostRecent();
                hit = true;
                balls[i].bTouchDown = false;
            }
            if(!hit && recBtn.hitTest(balls[i].pos))
            {
                record = !record;
                hit = true;
                balls[i].bTouchDown = false;
            }
        }
	}
    
}


//--------------------------------------------------------------
void testApp::draw() {
	
    ostringstream oss;
    
	ofEnableAlphaBlending();
    oss << "tool class: " << toolClassStrs[currentClass] << endl;
    oss << "num touches: " << numBallsDragging << endl;
    
	ofPushStyle();
    // draw disabled area
    ofSetColor(100, 100, 100);
    ofRect(0,0,ofGetWidth(), TOUCH_INVALID_AREA);
    
    // ofEnableBlendMode(OF_BLENDMODE_MULTIPLY);
    for(int i = 0; i< balls.size(); i++){
        if(balls[i].bDragged){
            balls[i].draw();
            oss << i << "("<< balls[i].pos.x << "," << balls[i].pos.y << "): " << balls[i].touchRadius <<endl;
        }
    }
    
    // draw rectangle in corner
    float divideBy = 10.0;
    ofSetColor(0,0,0);
    ofRect(0, 0, ofGetWidth() / divideBy, ofGetHeight() / divideBy);
    
    // draw balls at reduced size
    
    for(int i = 0; i< balls.size(); i++){
        if(balls[i].bDragged){
            balls[i].drawSmallWhite(divideBy);
        }
    }
	ofPopStyle();
    
    nextBtn.draw();
    prevBtn.draw();
    delBtn.draw();
    delLastBtn.draw();
    recBtn.draw();

    ofSetColor(54);
    ofDrawBitmapString(oss.str(), 10 + ofGetWidth() / divideBy, TOUCH_INVALID_AREA + 20);
    
    // draw info about number of files
    oss.str("");
    oss.clear();
    updateDocsDir();
    ofLog(OF_LOG_VERBOSE,"docs dir is %s", [docsDir UTF8String]);
    oss << "files in directory" << endl;
    NSArray *arr = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:docsDir error:nil];
    for(NSString *path in arr)
    {
        oss << [path UTF8String] << endl;
    }
    
    ofDrawBitmapString(oss.str(), ofGetWidth() - 300, TOUCH_INVALID_AREA + 20);
    
    if(record)
    {
        ofSetColor(0,255,0);
        ofDrawBitmapString("recording on", 10 + ofGetWidth() / divideBy, 20, 20);
    }
}

//--------------------------------------------------------------
void testApp::exit(){
    
}

//--------------------------------------------------------------
void testApp::touchDown(ofTouchEventArgs & touch){
    if(numBallsDragging == 0 && record && touch.y > TOUCH_INVALID_AREA)
        newTouchDir();
    
    numBallsDragging++;
    ofLog(OF_LOG_VERBOSE, "touch %d down at (%f,%f)", touch.id, touch.x, touch.y);
	balls[touch.id].moveTo(touch.x, touch.y);
    balls[touch.id].touchRadius = touch.majoraxis * TOUCH_AREA_MULTIPLIER;
	balls[touch.id].bDragged = true;
    balls[touch.id].bTouchDown = true;
    balls[touch.id].typeDragged = touch.type;
    
    if(touch.y < TOUCH_INVALID_AREA) return;
    
    if(record)writeTouchUpdated(touch);
}

// Delete most recently written file
void testApp::deleteMostRecent()
{
    updateDocsDir();
    NSArray *arr = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:docsDir error:nil];
    if(arr.count > 0)
    {
        NSString *p = arr.lastObject;
        NSString* abs = [docsDir stringByAppendingPathComponent:p];
        [[NSFileManager defaultManager] removeItemAtPath:abs error:nil];
    }
}


void testApp::deleteAll()
{
    updateDocsDir();
    NSArray *arr = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:docsDir error:nil];
    for(NSString *path in arr)
    {
        NSString* abs = [docsDir stringByAppendingPathComponent:path];
        [[NSFileManager defaultManager] removeItemAtPath:abs error:nil];
    }
}



//--------------------------------------------------------------
void testApp::touchMoved(ofTouchEventArgs & touch){ 
    ofLog(OF_LOG_VERBOSE, "touch %d moved at (%f,%f)\n\t major %.2f minor %.2f angle %.2f\n\t width %.2f height %.2f)",touch.id, touch.x, touch.y, touch.minoraxis, touch.majoraxis, touch.angle, touch.width, touch.height);
	balls[touch.id].moveTo(touch.x, touch.y);
    balls[touch.id].touchRadius = touch.majoraxis * TOUCH_AREA_MULTIPLIER;
	balls[touch.id].bDragged = true;
    balls[touch.id].typeDragged = touch.type;

    if(touch.y < TOUCH_INVALID_AREA) return;
    
    if(record) writeTouchUpdated(touch);
}

//--------------------------------------------------------------
void testApp::touchUp(ofTouchEventArgs & touch){
    numBallsDragging--;
//    if(numBallsDragging == 0)
//    {
//        nextClass();
//    }
    ofLog(OF_LOG_VERBOSE, "touch %d up at (%f,%f)", touch.id, touch.x, touch.y);
	balls[touch.id].bDragged = false;
    
    if(touch.y < TOUCH_INVALID_AREA) return;
    
    if(record && numBallsDragging > 0) writeTouchUpdated(touch);
}

//--------------------------------------------------------------
void testApp::touchDoubleTap(ofTouchEventArgs & touch){
    ofLog(OF_LOG_VERBOSE, "touch %d double tap at (%d,%d)", touch.id, touch.x, touch.y);
}

//--------------------------------------------------------------
void testApp::touchCancelled(ofTouchEventArgs & touch){
    
}

//--------------------------------------------------------------
void testApp::lostFocus(){
    
}

//--------------------------------------------------------------
void testApp::gotFocus(){
    
}

//--------------------------------------------------------------
void testApp::gotMemoryWarning(){
    
}

//--------------------------------------------------------------
void testApp::deviceOrientationChanged(int newOrientation){
    
}

//--------------------------------------------------------------
void testApp::gotMessage(ofMessage msg){
	
}

