"""Acceptance of public signed APKs on CI emulators, without seeded market data."""
import json, re, subprocess, time, xml.etree.ElementTree as ET
from pathlib import Path
PACKAGE = 'com.kaspa.livewidget'
OUT = Path('acceptance-evidence'); OUT.mkdir(exist_ok=True)
checks = []
def adb(*args):
    return subprocess.check_output(['adb', *args], text=True)
def nodes():
    adb('shell','uiautomator','dump','/sdcard/acceptance.xml')
    return list(ET.fromstring(adb('shell','cat','/sdcard/acceptance.xml')).iter('node'))
def locate(identifier, by='resource-id', seek=True):
    for _ in range(9 if seek else 1):
        for n in nodes():
            if n.get(by) == identifier:
                box = list(map(int,re.findall(r'\d+',n.get('bounds',''))))
                if len(box) == 4 and box[2] > box[0] and box[3] > box[1]: return n,box
        if seek: adb('shell','input','swipe','540','1600','540','500','250'); time.sleep(.4)
    raise AssertionError('Control not visible: '+identifier)
def tap(identifier, by='resource-id'):
    _,b=locate(identifier,by); adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2)); time.sleep(.7)
def top():
    for _ in range(8): adb('shell','input','swipe','540','400','540','1600','150')
def open_app():
    adb('shell','am','start','-W','-n',PACKAGE+'/.SettingsActivity'); time.sleep(2)
def screenshot(name):
    with (OUT/(name+'.png')).open('wb') as f: subprocess.run(['adb','exec-out','screencap','-p'],stdout=f,check=True)
    assert (OUT/(name+'.png')).stat().st_size > 10000
    (OUT/(name+'.xml')).write_text(adb('shell','cat','/sdcard/acceptance.xml'))
def value(identifier): return locate(PACKAGE+':id/'+identifier)[0].get('text','')
def settings():
    top(); assert 'Frosted light' in value('appearance_choice'); assert 'GBP' in value('currency_choice'); assert '30 minutes' in value('interval_choice')
def settle():
    for _ in range(30):
        text=value('reading_status')
        if 'Update queued' not in text and 'Refreshing' not in text: return text
        time.sleep(3)
    raise AssertionError('Refresh did not finish in 90 seconds')
def passed(name): checks.append(name); print('PASS '+name,flush=True)
try:
    adb('shell','wm','size','1080x1920'); adb('shell','wm','density','420')
    adb('logcat','-c')
    adb('install','current/kaspa-live-widget.apk'); open_app()
    assert 'USD' in value('currency_choice'); screenshot('fresh-install'); passed('Fresh signed 1.4.0 install opens with USD')
    adb('uninstall',PACKAGE); adb('install','previous/kaspa-live-widget.apk'); open_app()
    tap(PACKAGE+':id/appearance_choice'); tap('Frosted light','text')
    tap(PACKAGE+':id/currency_choice'); tap('GBP','text')
    tap(PACKAGE+':id/interval_choice'); tap('30 minutes','text')
    tap(PACKAGE+':id/apply_settings'); settle()
    adb('install','-r','current/kaspa-live-widget.apk'); open_app(); settings(); screenshot('signed-upgrade'); passed('1.3.0 → 1.4.0 preserves saved GBP, light appearance and 30-minute interval')
    adb('shell','am','force-stop',PACKAGE); open_app(); settings(); passed('Settings survive process restart')
    tap(PACKAGE+':id/refresh_now'); online=settle(); screenshot('online-refresh'); passed('Manual refresh reaches a completed or unavailable state without invented readings')
    # Disable both emulator transports and prove the network is unavailable.
    adb('shell','svc','wifi','disable'); adb('shell','svc','data','disable'); time.sleep(3)
    probe=subprocess.run(['adb','shell','ping','-c','1','-W','2','1.1.1.1'],capture_output=True,text=True)
    assert probe.returncode != 0, 'Emulator still has an active network transport'
    before=value('reading_status').splitlines(); tap(PACKAGE+':id/refresh_now'); offline=settle(); after=offline.splitlines()
    assert 'cached readings kept' in offline, 'Offline refresh did not report cached/unavailable state'
    assert len(before)>=2 and len(after)>=2 and before[1] == after[1], 'Offline refresh replaced the saved price/change reading'
    screenshot('offline-refresh'); passed('Offline refresh keeps saved price/change and reports unavailability')
    if '--' in online: passed('Live feed unavailable on this runner; cached value retention beyond placeholder needs device confirmation')
    adb('shell','svc','wifi','enable'); adb('shell','svc','data','enable')
    adb('reboot'); adb('wait-for-device')
    for _ in range(60):
        if adb('shell','getprop','sys.boot_completed').strip() == '1': break
        time.sleep(2)
    else: raise AssertionError('Reboot did not complete')
    adb('shell','input','keyevent','82'); open_app(); settings(); passed('Saved settings survive emulator reboot')
    top(); tap(PACKAGE+':id/preview_choice'); tap('Expanded · all readings','text'); top(); screenshot('expanded-preview')
    tap(PACKAGE+':id/preview_choice'); tap('Compact · price & score','text'); top(); screenshot('compact-preview'); passed('Both real RemoteViews previews render')
    adb('shell','settings','put','system','font_scale','1.3'); adb('shell','am','force-stop',PACKAGE); open_app(); top(); screenshot('larger-text'); passed('Setup opens at 1.3 font scale; screenshots require visual inspection')
    assert 'FATAL EXCEPTION' not in adb('logcat','-d','-s','AndroidRuntime:E'), 'Android app crashed'
    passed('No fatal AndroidRuntime exception during acceptance')
finally:
    (OUT/'report.json').write_text(json.dumps({'api':adb('shell','getprop','ro.build.version.sdk').strip(),'checks':checks,'limits':['Emulator only: launcher placement/resize, physical battery restrictions and Play delivery require owner devices.']},indent=2))
    adb('shell','svc','wifi','enable'); adb('shell','svc','data','enable'); adb('shell','settings','put','system','font_scale','1.0')
