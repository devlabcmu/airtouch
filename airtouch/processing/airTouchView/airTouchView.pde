import processing.net.*;
Client _myClient;

int PMD_FINGER_ONLY_DATA_SIZE = 32;

class Point3f
{
  float x;
  float y;
  float z;
}

Point3f[] _fingers = new Point3f[2];
byte[] _byteBuffer = new byte[PMD_FINGER_ONLY_DATA_SIZE];

void setup() {
  // adjust for macbook air
  size(1440, 900);

  // Connect to the local machine at port 5204.
  // This example will not run if you haven't
  // previously started a server on this port.
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
    println("read " + byteCount + " bytes");
    println(_byteBuffer);
    for(int i = 0; i < _fingers.length; i++){
      _fingers[i].x = getFloatInByteArray(_byteBuffer, i * 16 + 4);
      _fingers[i].y = getFloatInByteArray(_byteBuffer, i * 16 + 8);
      _fingers[i].z = getFloatInByteArray(_byteBuffer, i * 16 + 12);
      println( i + ": (" + _fingers[i].x + "," + _fingers[i].y + "," + _fingers[i].z + ")");  
    }   
  }
}


void draw() {
  update();
}

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

