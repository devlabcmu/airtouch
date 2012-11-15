#include "testApp.h"
#define TOUCH_AREA_MULTIPLIER 10



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
    
    // file io setup
    NSArray *dirPaths;
    filemgr =[NSFileManager defaultManager];
    
    updateDocsDir();
    
    ostringstream oss;
    oss << [docsDir UTF8String] ;
    oss << "/";
    ofSetDataPathRoot(oss.str());
    
    ofDirectory dir(OUT_DIR);
    // make directory
    if(!dir.exists())
    {
        dir.create();
    }
}


void testApp::newTouchDir()
{
    // date-time_class
    bool success;
    
    string cls = toolClassStrs[(int)currentClass];
    ostringstream oss;
    // eg: %Y-%m-%d-%H-%M-%S-%i ( 2011-01-15-18-29-35-299 )
    oss<< ofGetTimestampString("%Y%m%d-%h%M%S%i") << "_" << cls;
    string path = ofFilePath::join(OUT_DIR, oss.str());
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
    ofLog(OF_LOG_VERBOSE, "tfp is %s", touch_file_path.c_str());
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
	for(int i=0; i < balls.size(); i++){
		balls[i].update();
	}
}


//--------------------------------------------------------------
void testApp::draw() {
	
    ostringstream oss;
    
	ofEnableAlphaBlending();
    oss << "tool class: " << toolClassStrs[currentClass] << endl;
    oss << "num touches: " << numBallsDragging << endl;
    
	ofPushStyle();
    ofEnableBlendMode(OF_BLENDMODE_MULTIPLY);
    for(int i = 0; i< balls.size(); i++){
        if(balls[i].bDragged){
            balls[i].draw();
            oss << i << "("<< balls[i].pos.x << "," << balls[i].pos.y << "): " << balls[i].touchRadius <<endl;
        }
    }
	ofPopStyle();
    

    ofSetColor(54);
    ofDrawBitmapString(oss.str(), 10, 20);
}

//--------------------------------------------------------------
void testApp::exit(){
    
}

//--------------------------------------------------------------
void testApp::touchDown(ofTouchEventArgs & touch){
    if(numBallsDragging == 0)
        newTouchDir();
    
    numBallsDragging++;
    ofLog(OF_LOG_VERBOSE, "touch %d down at (%f,%f)", touch.id, touch.x, touch.y);
	balls[touch.id].moveTo(touch.x, touch.y);
    balls[touch.id].touchRadius = touch.majoraxis * TOUCH_AREA_MULTIPLIER;
	balls[touch.id].bDragged = true;
    balls[touch.id].typeDragged = touch.type;
    
    writeTouchUpdated(touch);
}

//--------------------------------------------------------------
void testApp::touchMoved(ofTouchEventArgs & touch){
    
    ofLog(OF_LOG_VERBOSE, "touch %d moved at (%f,%f)\n\t major %.2f minor %.2f angle %.2f\n\t width %.2f height %.2f)",touch.id, touch.x, touch.y, touch.minoraxis, touch.majoraxis, touch.angle, touch.width, touch.height);
	balls[touch.id].moveTo(touch.x, touch.y);
    balls[touch.id].touchRadius = touch.majoraxis * TOUCH_AREA_MULTIPLIER;
	balls[touch.id].bDragged = true;
    balls[touch.id].typeDragged = touch.type;
    
    writeTouchUpdated(touch);
}

//--------------------------------------------------------------
void testApp::touchUp(ofTouchEventArgs & touch){
    numBallsDragging--;
    if(numBallsDragging == 0)
    {
        nextClass();
    }
    ofLog(OF_LOG_VERBOSE, "touch %d up at (%f,%f)", touch.id, touch.x, touch.y);
	balls[touch.id].bDragged = false;
    
    writeTouchUpdated(touch);
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

