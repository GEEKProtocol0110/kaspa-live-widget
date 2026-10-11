# Physical-device trial

Automated signed APK acceptance runs the existing public 1.3.0 and 1.4.0 releases on Android API 26 and 36. It checks a fresh install, settings saved before upgrade, process restart, reboot, manual refresh, offline retention and both actual widget-layout previews. Artifacts contain reports, screenshots and view hierarchies. The app release is not changed by these tests. A passing emulator run does not certify a physical launcher or battery policy.

Owner record for each phone:

| Item | Record |
| --- | --- |
| Device / Android version / launcher | Not yet recorded |
| Fresh signed 1.4.0 install | Not yet checked |
| 1.3.0 update keeps settings and both home-screen widgets | Not yet checked |
| Compact and expanded placement / resize / portrait / landscape | Not yet checked |
| GBP and EUR; no USD cache relabeling | Not yet checked |
| Airplane mode keeps last reading and labels stale/unavailable | Not yet checked |
| Reboot keeps widgets and settings | Not yet checked |
| Default battery optimization; revisit after 30–60 minutes | Not yet checked |
| Android 16 gestures, cutout, large text and keyboard focus | Not yet checked |
| Play internal install updates APK with the same app signing identity | Not yet checked |

Use the permanent signed APK download in README and check release hashes. Do not uninstall before an update test, because uninstalling deletes saved settings. A screenshot must show the actual phone/emulator, not a mockup. Report failures with phone, Android, launcher, selected settings, network condition and steps; do not include private wallet information. Review the separate Play release checklist for owner account and submission requirements.
