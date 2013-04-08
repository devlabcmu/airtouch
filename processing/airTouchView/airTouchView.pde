import processing.net.*;
Client _myClient;

int PMD_FINGER_ONLY_DATA_SIZE = 32;
int PHONE_WIDTH = 450;
int PHONE_HEIGHT = 800;

int FINGER_WIDTH = 100;
int FINGER_HEIGHT = 100;

float PHONE_HEIGHT_M = 0.135;

class Point3f
{
  int id;
  float x;
  float y;
  float z;
}

Point3f[] _fingers = new Point3f[2];
byte[] _byteBuffer = new byte[PMD_FINGER_ONLY_DATA_SIZE];

// UI
color phoneColor1 = color(150, 150, 150);
color phoneColor2 = color(180, 180, 180);
color fingerColor = color(255, 0, 0);

void removeChrome() {
  frame.removeNotify();
  frame.setLocation(0, 0);
  frame.setUndecorated(true);
  frame.addNotify();
}

void setup() {
  // UI setup
  // adjust for macbook air
  size(1920, 1080);
  noStroke();
  removeChrome();

  for (int i = 0; i < _fingers.length; i++) _fingers[i] = new Point3f();

  // Network setup
  _myClient = new Client(this, "127.0.0.1", 10000);
  println("sending 'desktop' message");
  _myClient.write("desktop");
  println("receiving handshake...");
  if (_myClient.available() > 0) {
    String reply = _myClient.readString();
    println("received reply: " + reply);
  }
  else {
    println("client not available");
  }
}

void update()
{
  getFingerData();
}

void getFingerData() {
  if (_myClient.available() <= 0) return;
  // send 'f'
  _myClient.write("f");
  int byteCount = _myClient.readBytes(_byteBuffer);
  if (byteCount > 0) {
    for (int i = 0; i < _fingers.length; i++) {
      _fingers[i].id = getIntInByteArray(_byteBuffer, i*16);
      _fingers[i].x = getFloatInByteArray(_byteBuffer, i * 16 + 4);
      _fingers[i].y = getFloatInByteArray(_byteBuffer, i * 16 + 8);
      _fingers[i].z = getFloatInByteArray(_byteBuffer, i * 16 + 12);
      println( i + ": (" + _fingers[i].x + "," + _fingers[i].y + "," + _fingers[i].z + ")");
    }
  }
}


void drawTopView()
{
  fill(phoneColor2);
  rect(-20, -20, PHONE_WIDTH + 40, PHONE_HEIGHT + 100, 50);
  fill(phoneColor1);
  rect(0, 0, PHONE_WIDTH, PHONE_HEIGHT, 50);
  fill(fingerColor);
  for(int i = 0; i < _fingers.length; i++) {
    if(_fingers[i].id <0 ) continue;
    float x = _fingers[i].x * PHONE_WIDTH;
    float y = _fingers[i].z * PHONE_HEIGHT;
    ellipse(x,y,FINGER_WIDTH,FINGER_HEIGHT);
  }
  
}

void drawSideView()
{
  fill(phoneColor1);
  rect(0, 0, PHONE_HEIGHT, 50, 20);
  fill(fingerColor);
    for(int i = 0; i < _fingers.length; i++) {
    if(_fingers[i].id <0 ) continue;
    float x = _fingers[i].x * PHONE_WIDTH;
    float d = _fingers[i].y / PHONE_HEIGHT_M * PHONE_HEIGHT * 2;
    ellipse(x,-d,FINGER_WIDTH,FINGER_HEIGHT);
  }
}

void draw() {
  update();
  background(50, 50, 50);
  int xPad = (width / 2 - PHONE_WIDTH) / 2;
  int yPad = (height - PHONE_HEIGHT) / 2;
  pushMatrix();
  translate(xPad, yPad );
  drawTopView();
  popMatrix();
  pushMatrix();
  translate(width / 2, height / 2 + 200);
  drawSideView();
  popMatrix();
}



/// Utilities

/**
 * Gets a float at a specified offset from a byte array.
 * This should be a general utility
 * @param bytes
 * @param startOffset The start of the array to look at, in bytes. Shoudl increment in steps of 4 for floats
 * @return
 */
float getFloatInByteArray(byte[] bytes, int startOffset)
{
  // 0xF is 4 bits, 0xFF is 8 bits
  int asInt = (bytes[startOffset + 0] & 0xFF) 
    | ((bytes[startOffset + 1] & 0xFF) << 8) 
      | ((bytes[startOffset + 2] & 0xFF) << 16) 
        | ((bytes[startOffset + 3] & 0xFF) << 24);
  return Float.intBitsToFloat(asInt);
}

public static int getIntInByteArray(byte[] bytes, int startOffset)
{
  return (bytes[startOffset + 0] & 0xFF) 
    | ((bytes[startOffset + 1] & 0xFF) << 8) 
      | ((bytes[startOffset + 2] & 0xFF) << 16) 
        | ((bytes[startOffset + 3] & 0xFF) << 24);
}

