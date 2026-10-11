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
geometry = []

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
        # Small, slow drags reveal the footer without flinging the clock offscreen.
        adb('shell', 'input', 'swipe', str(screen[2] // 2), str(int(screen[3] * .7)),
            str(screen[2] // 2), str(int(screen[3] * .65)), '800')
    raise AssertionError('Visible control missing: ' + target)

def tap(target, text=False):
    _, box = find(target, text)
    adb('shell', 'input', 'tap', str((box[0] + box[2]) // 2), str((box[1] + box[3]) // 2))
    time.sleep(1)

def open_app():
    adb('shell', 'am', 'force-stop', PACKAGE)
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/com.kaspa.livewidget.SettingsActivity')
    time.sleep(2)

def show_whole_preview(size):
    # Accessibility bounds are clipped to the visible window. Compare their
    # height with the thumbnail's complete height before taking the screenshot.
    density = int(re.findall(r'density:\s*(\d+)', adb('shell', 'wm', 'density'))[-1]) / 160
    width_dp, height_dp = (250, 300) if size == 'expanded' else (140, 180)
    for _ in range(12):
        tree = nodes()
        frame = next(n for n in tree if n.get('resource-id', '').endswith(':id/widget_preview'))
        box = bounds(frame); screen = bounds(tree[0])
        scale = min(1, (box[2] - box[0]) / int(width_dp * density))
        complete_height = int(int(height_dp * density) * scale) + int(16 * density)
        if box[3] - box[1] >= complete_height - 2:
            return
        adb('shell', 'input', 'swipe', str(screen[2] // 2), str(int(screen[3] * .7)),
            str(screen[2] // 2), str(int(screen[3] * .65)), '800')
    raise AssertionError('Whole preview did not fit in the screenshot viewport')

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
            for size, label in [('compact', 'Compact · price & score'), ('expanded', 'Expanded · all readings')]:
                # Each case starts at the top. Bringing a preview footer into view
                # can scroll its chooser offscreen on the previous case.
                open_app()
                tap('preview_choice'); tap(label, True)
                show_whole_preview(size)
                name = viewport + '-' + font + '-' + size
                tree = capture(name)
                def visible(control):
                    candidates = [n for n in tree if n.get('resource-id', '').endswith(':id/' + control)]
                    assert len(candidates) == 1, 'Visible control missing: ' + control
                    node = candidates[0]; box = bounds(node)
                    assert len(box) == 4 and box[2] > box[0] and box[3] > box[1], control + ' has empty bounds'
                    return node, box
                frame, frame_box = visible('widget_preview')
                for control in ['widget_time', 'widget_price', 'widget_block_height', 'widget_status', 'widget_chart_label']:
                    node, box = visible(control)
                    assert node.get('text', '').strip(), control + ' has no visible text'
                    assert frame_box[0] <= box[0] < box[2] <= frame_box[2], control + ' extends outside preview'
                if size == 'expanded':
                    visible('widget_bps'); visible('widget_hashrate')
                geometry.append({'name': name, 'screen': bounds(tree[0]), 'preview': frame_box})
                checks.append(name)
                print('PASS ' + name, flush=True)
finally:
    capture('final-state')
    (OUT / 'report.json').write_text(json.dumps({'api': adb('shell', 'getprop', 'ro.build.version.sdk').strip(),
        'checks': checks, 'geometry': geometry,
        'limits': ['Debug build; real launcher placement and signed update acceptance are separate checks.']}, indent=2))
    subprocess.run(['adb', 'shell', 'settings', 'put', 'system', 'font_scale', '1.0'], check=False)
