"""Capture actual app screens on the workflow's Android emulator; no seeded feeds."""
import re, subprocess, time, xml.etree.ElementTree as ET
from pathlib import Path
PACKAGE = 'com.kaspa.livewidget'
OUT = Path('store-screenshots'); OUT.mkdir(exist_ok=True)
def adb(*args):
    return subprocess.check_output(['adb', *args], text=True)
def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/window.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/window.xml')).iter('node')
def tap(identifier, by='resource-id', seek=False):
    for attempt in range(7 if seek else 1):
        for node in nodes():
            if node.get(by) == identifier:
                x1,y1,x2,y2 = map(int, re.findall(r'\d+',node.get('bounds')))
                if x2 > x1 and y2 > y1:
                    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2)); time.sleep(.8); return
        if seek: adb('shell','input','swipe','540','1600','540','500','400'); time.sleep(.5)
    raise RuntimeError('Visible control not found: '+identifier)
def top():
    for _ in range(7): adb('shell','input','swipe','540','400','540','1600','200')
    time.sleep(1)
def shot(name):
    assert 'Kaspa Live Widget' in adb('shell','dumpsys','activity','activities') or PACKAGE in adb('shell','dumpsys','activity','activities')
    with (OUT/name).open('wb') as output: subprocess.run(['adb','exec-out','screencap','-p'],stdout=output,check=True)
    assert (OUT/name).stat().st_size > 10000
adb('shell','wm','size','1080x1920'); adb('shell','wm','density','420')
adb('shell','settings','put','global','window_animation_scale','0')
adb('shell','settings','put','global','transition_animation_scale','0')
adb('shell','am','start','-W','-n',PACKAGE+'/.SettingsActivity'); time.sleep(3)
tap(PACKAGE+':id/refresh_now', seek=True); time.sleep(20); top()
shot('phone-compact-dark.png')
tap(PACKAGE+':id/preview_choice'); tap('Expanded · all readings',by='text')
tap(PACKAGE+':id/appearance_choice',seek=True); tap('Frosted light',by='text')
tap(PACKAGE+':id/apply_settings',seek=True); top()
shot('phone-expanded-light.png')
# Re-opening confirms saved settings survived a real process restart.
adb('shell','am','force-stop',PACKAGE); adb('shell','am','start','-W','-n',PACKAGE+'/.SettingsActivity'); time.sleep(2)
assert any(n.get('text') == 'Frosted light  ▾' for n in nodes()), 'Saved theme did not survive restart'
assert 'FATAL EXCEPTION' not in adb('logcat','-d','-s','AndroidRuntime:E'), 'App crash on Android 16'
print('Captured compact/dark and expanded/light screens after signed APK update and settings restart check.')
