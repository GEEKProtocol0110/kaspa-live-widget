# Kaspa Live Widget for iPhone

Native SwiftUI companion app and WidgetKit home-screen widgets for iOS/iPadOS 17+. This is the first iPhone implementation; it is not a publicly installable iPhone release.

Includes small/medium/large layouts, the existing Kaspa mark, USD price with provider time and labeled KAS/USDT fallback, DAA score, observed average BPS and TH/s→PH/s hashrate. Medium/large widgets show actual 24h price observations behind the readings. Empty or invalid history produces no invented chart. Prices, network and hashrate retain separate observation times when a feed fails. BPS requires two successful samples 30 seconds–6 hours apart; it is a sample average, not a promise of current instantaneous throughput.

Native settings include neon dark/frosted light/X-ray/custom themes, seven color controls, chart intensity, 20 named local looks and requested refresh intervals. Loading a look changes a draft until Apply settings. The app and extension share settings/readings through `group.xyz.geekprotocol.kaspawidget`; feed refreshes use a different key from appearance settings.

## Build on a Mac

Install Xcode and XcodeGen, then from this directory:

```bash
swift test --package-path KaspaCore
brew install xcodegen
xcodegen generate
open KaspaLiveWidget.xcodeproj
```

Select the KaspaLiveWidget scheme and an iPhone Simulator, then Run. CI performs core tests, native app/extension builds and a launch/save/load/restart UI test. Its downloadable `.app` is for the Simulator only and cannot be installed on an iPhone. UI-test screenshots are real simulator evidence; no mock price history is inserted.

## Apple signing and public beta

1. Use the owner's Apple Developer Program account. Never paste certificates, private keys, passwords or App Store Connect API keys into issues or chat.
2. Register `xyz.geekprotocol.kaspawidget`, `xyz.geekprotocol.kaspawidget.home` and App Group `group.xyz.geekprotocol.kaspawidget` with the same Apple team. If a name is unavailable, update **both** targets, the group constant, URL name and CI scripts consistently. Both app and extension must have the same App Group entitlement in their provisioning profiles.
3. In Xcode set that team on both targets; keep automatic signing. Run on a physical iPhone; confirm group persistence, widget gallery placement for all three sizes, launch from widget, offline cached values, stale labels, two-sample BPS and settings reload after process restart.
4. Create an App Store Connect record, review the privacy policy/App Privacy answers against the actual providers, archive a signed device build and validate it. Upload for TestFlight, then complete Apple's review requirements for external testers. Do not publish an unsigned IPA or tell users an Android APK installs on iPhone.
5. Invite testers only when explicitly authorized by the owner, after the public beta build/link exists. App Store publication is a separate owner-approved step.

The privacy manifest declares App Group UserDefaults use (`1C8F.1`) and no app analytics/tracking SDK. Public price/network providers still receive network requests (including IP addresses); review provider policies before distribution. No account, wallet, payment or address permission is required by this implementation.

Apple controls WidgetKit's refresh budget. 15/30/60 minutes are requested intervals, never guaranteed live prices. Minute timeline entries advance the displayed clock and stale-state wording using the same cached observations; they do not fetch new values or renew their timestamps. Tapping a widget opens the app, where Refresh readings requests a new fetch.

Official references:
- https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date
- https://developer.apple.com/documentation/widgetkit/displaying-the-right-widget-background
- https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases
- https://developer.apple.com/documentation/bundleresources/app-privacy-configuration/nsprivacyaccessedapitypes/nsprivacyaccessedapitypereasons

First-version limits: only USD, REST network data (no public node/WebSocket fallback yet), no Android-to-iPhone settings migration, and no TestFlight/App Store release until signing/device acceptance. Apple tinted/StandBy appearance and larger accessibility text also require device acceptance.
