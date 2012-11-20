import pygame
import os
import csv
from collections import namedtuple

Touch = namedtuple('Touch', 'pos major')

# holder for visualization settings
class Settings:
    def __init__(self):
        self.timescale = 1.0 # increase to speed up, decrease to slow down
        self.paused = False

def enlarge(touch, w):
    return Touch(touch.pos, touch.major+w)

def parse_row(row):
    # remove spaces from row keys
    row = {k.strip():v for k,v in row.items()}
    touch = Touch((float(row['x']), float(row['y'])), float(row['major_axis']))
    return row['type'], int(row['id']), touch, int(row['time_start'])

def write_touches(dir, outf, t=0):
    with open(os.path.join(dir, 'touches.csv')) as f:
        csvf = csv.DictReader(f)
        touch_rows = iter(list(csvf))

    pts = {}
    t0 = None

    for row in touch_rows:
        type, id, touch, tm = parse_row(row)
        if t0 is None:
            t0 = tm
        tm -= t0

        if tm > t:
            break

        if type in ('TOUCH_DOWN', 'TOUCH_MOVE'):
            pts[id] = touch
        elif type == 'TOUCH_UP':
            del pts[id]

    dirname = os.path.basename(dir)
    timestamp, classlabel = dirname.split('_')

    out = [timestamp, classlabel]
    for i in xrange(12):
        if i in pts:
            out += [pts[i].pos[0], pts[i].pos[1], pts[i].major]
        else:
            out += [-1, -1, -1]

    print >>outf, ','.join(map(str, out))

if __name__ == '__main__':
    import sys
    if len(sys.argv) < 2:
        print "usage:", sys.argv[0], "[touchrecord directories...]"
        sys.exit(-1)

    with open('output.csv', 'wb') as outf:
        out = ['timestamp', 'classlabel']
        for i in xrange(12):
            out += ['x{i},y{i},w{i}'.format(i=i)]
        print >>outf, ','.join(out)
        for dir in sys.argv[1:]:
            write_touches(dir, outf, t=100)
