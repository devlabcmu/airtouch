from __future__ import division

import os
import csv
import numpy as np
import math
from gen_pts import get_touches
from collections import OrderedDict
import itertools

TEMPLATESIZE = 40
DISPW, DISPH = 1024, 768
DTIME = 100 # ms

def printline(line, eol=False):
    sys.stdout.write('{:<79}\r'.format(line))
    if eol:
        sys.stdout.write('\n')
    sys.stdout.flush()

def calc_list_features(out, l, tag):
    if len(l):
        out[tag + '_median'] = np.median(l)
        out[tag + '_mean'] = np.mean(l)
        out[tag + '_stdev'] = np.std(l)
        out[tag + '_min'] = np.min(l)
        out[tag + '_max'] = np.max(l)
    else:
        out[tag + '_median'] = 0
        out[tag + '_mean'] = 0
        out[tag + '_stdev'] = 0
        out[tag + '_min'] = 0
        out[tag + '_max'] = 0

def norm(pt):
    return sum(pt**2)**.5

def calc_features(d, ordered_dict=False):
    if ordered_dict:
        out = OrderedDict()
    else:
        out = {}

    timestamp, pts, classlabel = get_touches(d, t=DTIME)
    pts = [(pt.pos[0], pt.pos[1], pt.major) for pt in pts.values()]

    np_pts = np.array([pt[:2] for pt in pts])
    centroid = np_pts.mean(axis=0)

    calc_list_features(out, [norm(pt1-pt2) for pt1, pt2 in itertools.combinations(np_pts, 2)], 'allpair_dist')
    calc_list_features(out, [norm(pt) for pt in (np_pts - centroid)], 'centroid_dist')
    angles = [math.atan2(pt[1], pt[0]) for pt in (np_pts - centroid)]
    angles.sort()
    anglediffs = [(b-a) % (2*math.pi) for a,b in zip(angles, angles[1:] + angles[:1])]
    calc_list_features(out, anglediffs, 'centroid_anglediff')
    pt_majors = np.array([pt[2] for pt in pts])
    calc_list_features(out, pt_majors, 'major')

    if len(np_pts) > 1:
        pt_c_weighted = ((np_pts - centroid) * pt_majors[:,None])
        U, S, V = np.linalg.svd(pt_c_weighted)
        out['ptcloud_major'], out['ptcloud_minor'] = S
        out['ptcloud_ratioLog'] = min(np.log(S[0]) - np.log(S[1]), 20)
    else:
        out['ptcloud_major'] = out['ptcloud_minor'] = pt_majors[0]
        out['ptcloud_ratioLog'] = 0

    out['touchcount'] = len(pts)
    out['major2_total'] = (pt_majors**2).sum()
    out['major_total'] = pt_majors.sum()
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
