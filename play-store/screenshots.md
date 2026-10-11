# Real phone screenshots

The release workflow's `screenshots` job installs the previous public APK on an Android 16 emulator, then installs the new release APK as an update. It captures the real setup activity with compact/dark and expanded/light previews, verifies that the app has not crashed, and attaches phone screenshots to the GitHub release. Values come from the app's actual public refresh or its honest unavailable state; no prices are inserted for marketing.

Inspect images for loading states, clipped text, system dialogs and readability before uploading to Google Play. Use at least two real phone screenshots. The capture script takes 1080×1920 PNG images, so the phone images satisfy the 320–3840px dimension range and 2:1 aspect-ratio maximum. Emulator navigation bars are part of the real screen. The icon and feature graphic in assets/ are brand graphics, not screenshots.

For additional real-device screenshots: install the signed APK, refresh, add both widgets, use a clean launcher without private notifications, show dark and light options, and take Android screenshots. Include a home-screen view of actual compact and expanded widgets if your launcher supports both. Do not upload repository Robolectric preview renders as phone screenshots.
