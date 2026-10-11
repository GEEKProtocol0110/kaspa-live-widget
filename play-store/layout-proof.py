"""Real debug-build preview evidence; no invented quotes or network readings."""
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = 'com.kaspa.livewidget.debug'
OUT = Path('layout-evidence')
OUT.mkdir(exist_ok=True)
checks = []

def adb(*args):
    return subprocess.check_output(['adb', *args], text=True)

def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/layout.xml')
    return list(ET.fromstring(adb('shell', 'cat', '/sdcard/layout.xml')).iter('node'))

def bounds(node):
    return list(map(int, re.findall(r'\d+', node.get('bounds', ''))))

def find(target, text=False):
    for _ in range(9):
        tree = nodes()
        for node in tree:
            match = node.get('text') == target if text else node.get('resource-id', '').endswith(':id/' + target)
            box = bounds(node)
            if match and len(box) == 4 and box[2] > box[0] and box[3] > box[1]:
                return node, box
        screen = bounds(tree[0])
        adb('shell', 'input', 'swipe', str(screen[2] // 2), str(int(screen[3] * .8)),
            str(screen[2] // 2), str(int(screen[3] * .3)), '250')
    raise AssertionError('Visible control missing: ' + target)

def tap(target, text=False):
    _, box = find(target, text)
    adb('shell', 'input', 'tap', str((box[0] + box[2]) // 2), str((box[1] + box[3]) // 2))
    time.sleep(1)

def open_app():
    adb('shell', 'am', 'force-stop', PACKAGE)
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/com.kaspa.livewidget.SettingsActivity')
    time.sleep(2)

def capture(name):
    tree = nodes()
    (OUT / (name + '.xml')).write_text(adb('shell', 'cat', '/sdcard/layout.xml'))
    with (OUT / (name + '.png')).open('wb') as stream:
        subprocess.run(['adb', 'exec-out', 'screencap', '-p'], stdout=stream, check=True)
    return tree

try:
    apk, = Path('layout-apk').glob('*.apk')
    adb('install', str(apk))
    for viewport, resolution in [('narrow', '640x1280'), ('wide', '1080x1920')]:
        adb('shell', 'wm', 'size', resolution)
        adb('shell', 'wm', 'density', '420')
        time.sleep(8)
        for font in ['1.0', '1.3']:
            adb('shell', 'settings', 'put', 'system', 'font_scale', font)
            open_app()
            for size, label in [('compact', 'Compact · price & score'), ('expanded', 'Expanded · all readings')]:
                tap('preview_choice'); tap(label, True)
                name = viewport + '-' + font + '-' + size
                tree = capture(name)
                frame, frame_box = find('widget_preview')
                for control in ['widget_time', 'widget_price', 'widget_block_height', 'widget_status', 'widget_chart_label']:
                    node, box = find(control)
                    assert node.get('text', '').strip(), control + ' has no visible text'
                    assert frame_box[0] <= box[0] < box[2] <= frame_box[2], control + ' extends outside preview'
                if size == 'expanded':
                    find('widget_bps'); find('widget_hashrate')
                checks.append(name)
                print('PASS ' + name, flush=True)
finally:
    capture('final-state')
    (OUT / 'report.json').write_text(json.dumps({'api': adb('shell', 'getprop', 'ro.build.version.sdk').strip(),
        'checks': checks, 'limits': ['Debug build; real launcher placement and signed update acceptance are separate checks.']}, indent=2))
    subprocess.run(['adb', 'shell', 'settings', 'put', 'system', 'font_scale', '1.0'], check=False)
