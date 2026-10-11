# Play release preparation

Prepared app: `com.kaspa.livewidget`, version 1.4.1, code 8, minimum Android 8/API 26, target Android 16/API 36. The tag release workflow builds the signed APK and AAB with the existing private signing identity; checksums accompany both. Keep the keystore and passwords private.

1. Register/verify the owner's Play Console account. Registration/payment and identity/device verification must be completed by the owner.
2. Create a free Tools app, confirm developer contact details and complete the content rating, target audience, app access and data-safety forms. Use listing.md and data-safety.md as reviewed preparation, not submitted declarations.
3. **Preserve the existing app signing certificate.** During first Play App Signing setup, use the option to provide your existing signing key. Follow Google's PEPK import instructions privately from the backed-up keystore. A different Google-generated app signing key would prevent Play installs from updating the public APK currently installed by users. The bundle's upload signature alone does not configure Play's final APK signing identity. A separate upload key can be adopted later through Play's supported process.
4. Upload `kaspa-live-widget.aab` from the verified v1.4.1 GitHub release into internal testing. Compare Play's app signing certificate against the existing public release certificate before distributing. Test an update from v1.3.0 and a fresh install; saved currency/theme/widget placement should remain.
5. Upload assets/icon-512.png and assets/feature-1024x500.png. Add at least two real phone screenshots from the screenshot workflow or the real-device procedure in screenshots.md. Inspect every image before submission; never represent a mockup or test-render as a device screenshot.
6. Test Android 8 and Android 16, compact/expanded widgets, both themes, EUR/GBP, no network, refresh, reboot and launcher resize. Include gesture navigation and display cutout checks on Android 16.
7. If Play requires a closed test for the account, recruit the required testers and complete its duration before applying for production access. New personal accounts generally require at least 12 opted-in testers for 14 continuous days; the owner's Console is authoritative for account-specific requirements.
8. Review and submit from the owner's Console. This repository does not publish to Play or make policy declarations automatically.

References checked October 11, 2026:
- https://developer.android.com/google/play/requirements/target-sdk
- https://support.google.com/googleplay/android-developer/answer/9842756
- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/10787469
- https://support.google.com/googleplay/android-developer/answer/9866151

## Release acceptance evidence

[Signed APK acceptance](../.github/workflows/acceptance.yml) tests fresh install and a real signed 1.3.0 → 1.4.0 update on API 26 and 36, preserving GBP/light/30-minute settings before and after process restart and reboot. It also exercises manual/offline refresh and actual compact/expanded previews. Download each run’s artifacts for the report and screenshots. Physical launcher resize, battery restrictions and Play-delivered updates remain in the [device trial worksheet](device-trial.md). These steps are not marked complete merely because CI passed.
