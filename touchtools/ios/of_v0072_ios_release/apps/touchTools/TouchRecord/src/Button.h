//
//  Button.h
//  TouchRecord
//
//  Created by Julia Schwarz on 11/20/12.
//
//

#ifndef TouchRecord_Button_h
#define TouchRecord_Button_h

class Button {
private:
    int _x;
    int _y;
    int _w;
    int _h;
    ofColor _c;
    string _txt;
    
public:
    void init(string txt, int x, int y, int w, int h, ofColor c)
    {
        _x = x;
        _y = y;
        _w = w;
        _h = h;
        _c = c;
        _txt = txt;
    }
    
    bool hitTest(ofPoint p)
    {
        float rx = p.x - _x;
        float ry = p.y - _y;
        return rx > 0 && rx < _w && ry > 0 && ry < _h;
    }
    
    void draw()
    {
        ofSetColor(_c);
        ofRect(_x, _y, _w, _h);
        ofSetColor(255, 255, 255);
        ofDrawBitmapString(_txt, _x + 10, _y + _h / 2 );
    }

};


#endif
