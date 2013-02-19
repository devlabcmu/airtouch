#!/usr/bin/env python
import pygame
import os
import csv
from collections import namedtuple

DISPLAY_SIZE = 1024,768

Touch = namedtuple('Touch', 'pos major')

# holder for visualization settings
class Settings:
    def __init__(self):
        self.timescale = 1.0 # increase to speed up, decrease to slow down
        self.paused = False

pygame.init()
screen = pygame.display.set_mode(DISPLAY_SIZE)

def enlarge(touch, w):
    return Touch(touch.pos, touch.major+w)

def render_circles(pts, colors):
    for id, touch in pts.items():
        pygame.draw.circle(screen, colors[id%len(colors)], map(int, touch.pos), int(touch.major))

def parse_row(row):
    # remove spaces from row keys
    row = {k.strip():v for k,v in row.items()}
    touch = Touch((float(row['x']), float(row['y'])), float(row['major_axis']))
    return row['type'], int(row['id']), touch, int(row['time_start'])

def visualize_touches(fn, settings, t=0):
    if not os.path.isdir(fn):
        return

    with open(os.path.join(fn, 'touches.csv')) as f:
        csvf = csv.DictReader(f)
        touch_rows = iter(list(csvf))

    font = pygame.font.SysFont('Arial Unicode MS', 12)
    clock = pygame.time.Clock()
    pts = {}
    downs = set()
    t0 = None
    next_row = next(touch_rows)
    pygame.key.set_repeat(300, 25)

    while 1:
        tick = clock.tick(60)*settings.timescale # 60 fps
        if not settings.paused:
            t += tick
            downs.clear()

        for event in pygame.event.get():
            if event.type == pygame.QUIT or (event.type == pygame.KEYDOWN and event.key == pygame.K_ESCAPE):
                return 'stop'
            elif event.type == pygame.KEYDOWN:
                if event.key == pygame.K_ESCAPE:
                    return 'stop'
                elif event.unicode == u' ':
                    settings.paused = not settings.paused
                elif event.unicode in u'+=':
                    settings.timescale *= 1.1
                elif event.unicode == u'-':
                    settings.timescale /= 1.1
                elif event.key == pygame.K_RIGHT:
                    t += 10
                    downs.clear()
                elif event.key == pygame.K_UP:
                    t += 100
                    downs.clear()
                elif event.key == pygame.K_HOME:
                    if t < 250:
                        return 'rewind'
                    else:
                        return 'restart'
                elif event.key == pygame.K_END:
                    return 'forward'

        while 1:
            type, id, touch, tm = parse_row(next_row)
            # Calculate time since start of gesture
            if t0 is None:
                t0 = tm
            tm -= t0

            if tm > t:
                break # not yet

            if type == 'TOUCH_DOWN':
                downs.add(id)

            if type in ('TOUCH_DOWN', 'TOUCH_MOVE'):
                pts[id] = touch
            elif type == 'TOUCH_UP':
                downs.discard(id)
                del pts[id]

            try:
                next_row = next(touch_rows)
            except StopIteration:
                next_row = None
                return 'done'

        screen.fill((255,255,255))
        x, y = 10, 10
        for line in [
            os.path.basename(fn),
            "Speed: %d%%" % round(settings.timescale * 100),
            "Time: %d" % t,
            "Paused" * settings.paused
        ]:
            textsurf = font.render(line, 1, (0, 0, 0))
            screen.blit(textsurf, (x, y))
            y += 20

        render_circles({id: enlarge(pts[id], 5) for id in downs}, [(127, 127, 127)])
        render_circles(pts, [(0,0,255), (255,0,0), (0,255,0), (0,255,255)])
        pygame.display.flip()

if __name__ == '__main__':
    import sys
    if len(sys.argv) < 2:
        print "usage:", sys.argv[0], "[touchrecord directories...]"
        sys.exit(-1)

    fns = sys.argv[1:]
    ind = 0
    settings = Settings()
    while 1:
        action = visualize_touches(fns[ind], settings, t=100) # , t=100 (to start partway into the gesture)
        if action == 'stop':
            break
        elif action == 'rewind':
            ind = (ind - 1) % len(fns)
        elif action == 'restart':
            pass
        elif action in ('forward', 'done'):
            ind = (ind + 1) % len(fns)
        else:
            raise ValueError("bad action " + action)
