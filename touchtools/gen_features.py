from __future__ import division

import os
import csv
import numpy as np
import pygame # to render images
from gen_pts import get_touches
from collections import OrderedDict

TEMPLATESIZE = 40
DISPW, DISPH = 1024, 768

def printline(line, eol=False):
    sys.stdout.write('{:<79}\r'.format(line))
    if eol:
        sys.stdout.write('\n')
    sys.stdout.flush()

def calc_features(d, ordered_dict=False):
    if ordered_dict:
        out = OrderedDict()
    else:
        out = {}

    timestamp, pts, classlabel = get_touches(d, t=250)
    pts = [(pt.pos[0], pt.pos[1], pt.major) for pt in pts.values()]

    # Get bounding box
    rects = [pygame.Rect(x-w/2, y-w/2, w, w) for (x,y,w) in pts]
    bbox = rects[0].unionall(rects)

    # Render points to image
    img = pygame.Surface((TEMPLATESIZE, TEMPLATESIZE), 0, 32)
    img.fill((0,0,0))
    wr = TEMPLATESIZE / bbox.w
    hr = TEMPLATESIZE / bbox.h
    for x,y,w in pts:
        ex = ((x - w/2) - bbox.left) * wr
        ey = ((y - w/2) - bbox.top) * hr
        ew = max(w*wr, 4)
        eh = max(w*hr, 4)
        pygame.draw.ellipse(img, (255,255,255), (ex, ey, ew, eh))
    # pygame.image.save(img, "test-%s.png" % timestamp)

    # Put image in CSV
    arr = (pygame.surfarray.pixels2d(img) & 0x0000ff00) >> 15
    for y in xrange(TEMPLATESIZE):
        for x in xrange(TEMPLATESIZE):
            out['pixel_r%d_c%d' % (y,x)] = arr[y,x]

    out['touchcount'] = len(pts)
    out['pixelsum'] = arr.sum()
    out['bbox_width'] = bbox.width
    out['bbox_height'] = bbox.height
    out['major_total'] = sum(pt[2] for pt in pts)
    out['major_avg'] = out['major_total'] / len(pts)
    out['classlabel'] = classlabel
    return out

def write_features(progressline, f, dirs, **opts):
    csvwriter = None
    for inputdir in dirs:
        dirs = os.listdir(inputdir)
        for di,d in enumerate(dirs):
            printline(progressline + '{}: {}/{}'.format(inputdir, di+1, len(dirs)))
            d = os.path.join(inputdir, d)
            if not os.path.isdir(d):
                continue
            try:
                row = calc_features(d, ordered_dict=csvwriter is None, **opts)
            except Exception as e:
                print d, "failed:", e.__class__.__name__+':', e
                continue

            if row is None:
                continue

            if csvwriter is None:
                csvwriter = csv.DictWriter(f, fieldnames=list(row))
                csvwriter.writeheader()
            csvwriter.writerow(row)

    printline(progressline + 'Done.', eol=True)

def parse_args(argv):
    import argparse
    parser = argparse.ArgumentParser(description="Generate features from raw data")
    parser.add_argument('-o', '--output', help="Output directory for generated features. Defaults to last data directory.")
    parser.add_argument('dirs', nargs='+', help="Directories containing data point directories")
    return parser.parse_args(argv)

def main(argv):
    args = parse_args(argv)

    if args.output is None:
        args.output = args.dirs[-1]

    try:
        os.mkdir(args.output)
    except EnvironmentError:
        pass

    with open(os.path.join(args.output, 'output.csv'), 'w') as f:
        colvals = write_features("generating features: ", f, args.dirs)

if __name__ == '__main__':
    import sys
    exit(main(sys.argv[1:]))
