"""Exercise the native X-ray/custom editor and saved colors on the real debug APK."""
import importlib.util
import json
import time
import xml.etree.ElementTree as ET
from pathlib import Path

spec = importlib.util.spec_from_file_location('layout', Path(__file__).with_name('layout-proof.py'))
ui = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ui)
ui.OUT = Path('theme-evidence'); ui.OUT.mkdir(exist_ok=True)
checks = []

def find(target, text=False):
    for _ in range(12):
        tree = ui.nodes()
        for node in tree:
            match = node.get('text') == target if text else node.get('resource-id', '').endswith(':id/' + target)
            box = ui.bounds(node)
            if match and len(box) == 4 and box[2] > box[0] and box[3] > box[1]: return node, box
        screen = ui.bounds(tree[0])
        # Scroll in the outer gutter: a drag starting on a Spinner is consumed
        # by that control and does not reveal the lower editor/save controls.
        x = str(int(screen[0] + (screen[2] - screen[0]) * .96))
        start = str(int(screen[1] + (screen[3] - screen[1]) * .8))
        end = str(int(screen[1] + (screen[3] - screen[1]) * .52))
        ui.adb('shell', 'input', 'swipe', x, start, x, end, '700')
    raise AssertionError('Visible control missing: ' + target)

ui.find = find

def top():
    screen = ui.bounds(ui.nodes()[0])
    for _ in range(6):
        ui.adb('shell', 'input', 'swipe', str(int(screen[2] * .96)), str(int(screen[3] * .25)),
            str(int(screen[2] * .96)), str(int(screen[3] * .8)), '500')

def appearance(index, label):
    ui.tap('appearance_choice')
    ui.adb('shell', 'input', 'keyevent', *(['19'] * 4 + ['20'] * index + ['66']))
    time.sleep(1)
    selected, _ = ui.find('appearance_choice')
    assert label in ' '.join(n.get('text', '') for n in selected.iter('node'))

def color(control, value):
    ui.tap(control); ui.tap('color_hex')
    ui.adb('shell', 'input', 'keyevent', '123', *(['67'] * 7))
    ui.adb('shell', 'input', 'text', value)
    time.sleep(1)
    input_node, _ = ui.find('color_hex')
    assert input_node.get('text', '').upper() == value.upper()
    ui.adb('shell', 'input', 'keyevent', '4')  # Hide the keyboard, retaining the dialog.
    # Exercise an actual user slider event, not just programmatic RGB updates.
    _, blue = ui.find('color_blue')
    target_blue = '00' if value[-2:].upper() == 'FF' else 'FF'
    slider_x = blue[0] + 1 if target_blue == '00' else blue[2] - 1
    ui.adb('shell', 'input', 'tap', str(slider_x), str((blue[1] + blue[3]) // 2))
    input_node, _ = ui.find('color_hex')
    assert input_node.get('text', '').upper() == '#' + value[:4].upper() + target_blue, 'RGB slider did not update hex: ' + input_node.get('text', '')
    ui.tap('color_hex')
    ui.adb('shell', 'input', 'keyevent', '123', *(['67'] * 7))
    ui.adb('shell', 'input', 'text', value)
    ui.adb('shell', 'input', 'keyevent', '4')
    ui.capture('editor-' + control)
    ui.tap('Use color', text=True)

def preferences():
    xml = ui.adb('shell', 'run-as', ui.PACKAGE, 'cat', 'shared_prefs/kaspa_widget_settings.xml')
    return {n.get('name'): n.get('value', n.text) for n in ET.fromstring(xml)}

def passed(value):
    checks.append(value); print('PASS ' + value, flush=True)

try:
    ui.adb('shell', 'wm', 'size', '1080x1920'); ui.adb('shell', 'wm', 'density', '420')
    ui.adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0'); time.sleep(8)
    ui.open_app(); appearance(2, 'X-ray')
    ui.tap('apply_settings')
    assert preferences()['theme'] == 'xray'
    ui.open_app()
    for size, label in [('compact', 'Compact · price & score'), ('expanded', 'Expanded · all readings')]:
        top(); ui.select_preview(size, label); ui.show_whole_preview(size)
        ui.capture('xray-' + size)
    passed('Saved X-ray theme renders both actual previews after process restart')
    appearance(3, 'Custom colors')
    color('color_background', '140C24'); color('color_text', 'F5F5FF'); color('color_accent', 'FF4FD8')
    # Preview changes do not persist until the explicit save action.
    assert preferences()['theme'] == 'xray'
    ui.tap('apply_settings')
    saved = preferences()
    expected = {'color_background': '140C24', 'color_text': 'F5F5FF', 'color_accent': 'FF4FD8'}
    assert saved['theme'] == 'custom'
    for key, value in expected.items(): assert int(saved[key]) & 0xFFFFFF == int(value, 16)
    ui.open_app()
    for size, label in [('compact', 'Compact · price & score'), ('expanded', 'Expanded · all readings')]:
        top(); ui.select_preview(size, label); ui.show_whole_preview(size)
        ui.capture('custom-' + size)
    assert preferences() == saved
    passed('Native color editor saves exact RGB colors and retains them after process restart')
    assert 'FATAL EXCEPTION' not in ui.adb('logcat', '-d', '-s', 'AndroidRuntime:E')
    passed('No fatal AndroidRuntime exception during theme editing')
finally:
    ui.capture('final-state')
    (ui.OUT / 'report.json').write_text(json.dumps({'api': ui.adb('shell', 'getprop', 'ro.build.version.sdk').strip(),
        'checks': checks, 'limits': ['Debug emulator previews; physical launcher and signed update verification are separate checks.']}, indent=2))
