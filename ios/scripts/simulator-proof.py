"""Run native UI tests and capture the real simulator; no invented market values."""
import json
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
out = root / 'evidence'
out.mkdir(exist_ok=True)
devices = json.loads(subprocess.check_output(['xcrun', 'simctl', 'list', 'devices', 'available', '-j']))['devices']
phones = [(runtime, device) for runtime, group in devices.items() if 'iOS' in runtime for device in group if device['name'].startswith('iPhone') and device.get('isAvailable')]
assert phones, 'No available iPhone Simulator'
runtime, device = sorted(phones, key=lambda item: (item[0], item[1]['name']), reverse=True)[0]
udid = device['udid']
subprocess.run(['xcrun', 'simctl', 'boot', udid], check=False)
subprocess.run(['xcrun', 'simctl', 'bootstatus', udid, '-b'], check=True, timeout=300)
command = ['xcodebuild', '-project', str(root / 'KaspaLiveWidget.xcodeproj'), '-scheme', 'KaspaLiveWidget',
           '-destination', 'platform=iOS Simulator,id=' + udid, '-derivedDataPath', str(root / 'build'),
           '-resultBundlePath', str(out / 'UITests.xcresult'), 'CODE_SIGNING_ALLOWED=NO', 'test']
subprocess.run(command, check=True, timeout=900)
app = root / 'build/Build/Products/Debug-iphonesimulator/KaspaLiveWidget.app'
assert (app / 'PlugIns/KaspaHomeWidget.appex').exists(), 'Widget extension was not embedded'
subprocess.run(['xcrun', 'simctl', 'launch', udid, 'xyz.geekprotocol.kaspawidget'], check=True)
subprocess.run(['xcrun', 'simctl', 'io', udid, 'screenshot', str(out / 'iphone-app.png')], check=True)
(out / 'report.json').write_text(json.dumps({'device': device['name'], 'runtime': runtime,
    'checks': ['Core regression suite passed before simulator stage', 'Native app and embedded WidgetKit extension compile', 'UI launch/save/load/restart test passed'],
    'limits': ['Unsigned Simulator build only. Physical widget placement, signed App Group access and TestFlight/App Store distribution require Apple developer signing and device acceptance.']}, indent=2))
