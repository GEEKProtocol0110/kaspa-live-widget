<div align="center">

# Kaspa Live Widget

Native iPhone support is being developed in [`ios/`](ios/README.md): a SwiftUI app and three WidgetKit sizes. This source is not a public iPhone download. Apple signing, physical-device acceptance and TestFlight distribution are still required; the Android APK remains Android-only.

[![License](https://img.shields.io/github/license/GEEKProtocol0110/kaspa-live-widget)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8-blue.svg)](https://kotlinlang.org)
[![Android CI](https://github.com/GEEKProtocol0110/kaspa-live-widget/actions/workflows/ci.yml/badge.svg)](https://github.com/GEEKProtocol0110/kaspa-live-widget/actions/workflows/ci.yml)

**A free, open-source Android home screen widget that displays live Kaspa network data.**

[Features](#features) • [Installation](#installation) • [Building](#building) • [Documentation](#documentation) • [Contributing](#contributing)

</div>

---

## Features

**iPhone users:** a separate [Scriptable version](scriptable/README.md) is available as a single copy/paste script, with small, medium, and large Home Screen layouts. It needs the free Scriptable app and no TestFlight membership. Automated checks cover the script; physical iPhone validation is pending.

- 📱 **Two Widget Sizes**: 2x2 (quick glance) and 4x4 (expanded view)
- 🎨 **Unified Design**: Kaspa media-kit mark at the top, neon recorded-price line and frosted-glass readings on both sizes
- 💰 **Live KAS Price**: USD, EUR or GBP with real 24-hour change; CoinGecko plus a USD-only exchange fallback
- 📊 **Network Stats**: DAA score, observed average BPS (after two readings), and hashrate in TH/s, PH/s, or EH/s
- ⏰ **Current Time**: Launcher-driven clock follows your phone’s 12/24-hour preference
- 🔄 **Auto Updates**: Choose 15, 30 or 60 minutes using WorkManager; tap for visible refresh feedback
- 💾 **Honest Freshness**: Keeps each feed’s last good reading and timestamp during partial outages; labels stale price, network, and hashrate readings
- 🎨 **Clean Design**: Cypherpunk graphite surface, mint/cyan glow, etched grid and digital typography

## Setup and price history

Open the Kaspa Live Widget app icon or tap **Setup** on a widget. Preview either widget size, choose USD/EUR/GBP, a 15/30/60-minute refresh interval, dark/light appearance and an optional price chart with **1H, 24H, 7D or 30D** history. The preview changes immediately using saved readings; tap **Apply settings** to save, or **Save & add** to save and request a home-screen widget. Unsaved choices survive phone rotation. These settings apply to every instance. If your launcher does not support the Add widget buttons, long-press your home screen and use its Widgets menu.

Tap the widget or **Refresh saved settings** for queued/running/completed/partial-failure feedback. Repeated taps share an unfinished request. Failed feeds keep their last successful timestamps. A currency switch never relabels a cached dollar value as euros or pounds. Exchange fallback is KAS/USDT (approximately USD) and cannot fill EUR/GBP. A price-only source shows “24h change unavailable” instead of attaching an old change percentage.

Chart points come from CoinGecko’s real price history for the selected range, refreshed at most once per hour when enabled. Choose **Chart time range** in Setup, then **Apply settings**. The 1H view uses observations from the daily feed; 7D and 30D request their own history. Each currency and range keeps its own saved series. The range controls the chart window; the separate 15/30/60-minute setting controls background refresh, and the percentage beside the price remains explicitly a 24-hour change. Missing data shows unavailable; old series remain labelled stale. The neon line is fused into the background of both widget sizes, behind the glass panels. The setup screen previews the actual widget layout, including its clock and glass panels, without fetching feeds or saving draft choices. The preview is reused while readings/settings stay unchanged; manual refresh is disabled while a request is in progress. New installs enable the chart by default; an explicitly saved off setting stays off. Without valid history, the surface keeps its grid and shows the selected range as unavailable without drawing a price line.

The [signed public APK](https://github.com/GEEKProtocol0110/kaspa-live-widget/releases/latest/download/kaspa-live-widget.apk) is available without a GitHub account. See [SIGNED_RELEASES.md](SIGNED_RELEASES.md) for release verification and maintainer signing instructions.

## Release acceptance

[Automated signed APK checks](https://github.com/GEEKProtocol0110/kaspa-live-widget/actions/workflows/acceptance.yml) exercise Android 8 and Android 16 installs, upgrades, saved settings, restart/reboot and offline behavior. See the [physical-device trial](play-store/device-trial.md) and [Play checklist](play-store/release-checklist.md) for remaining owner checks. This widget is a companion to [GEEK HQ](https://www.geekprotocol.xyz/).

## Widget Layouts

### 2x2 Widget (Quick Glance)
**Purpose**: Minimal information at a glance
- Graphite glass surface with the proper Kaspa mark at the top
- Current time (top)
- KAS price (center focus, in dark panel)
- DAA score (bottom panel)
- No clutter, maximum clarity

### 4x4 Widget (Command View)
**Purpose**: Expanded view with more breathing room
- Same Kaspa header and neon glass surface
- Time + "Last updated" indicator (top row)
- Expanded KAS price (center)
- DAA score + observed BPS (middle row, side-by-side panels)
- Network hashrate (bottom panel)
- More space, not more noise

## Technical Details

### Architecture
- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36 (Android 16)
- **Update Mechanism**: AppWidgetProvider + WorkManager
- **Update Interval**: 15, 30 or 60 minutes; Android can defer background work
- **Manual Refresh**: Tap either widget; repeated taps share an unfinished request

### Data Sources
- **Price Data**: Multiple sources with automatic fallback
  - Primary: CoinGecko KAS/USD (public, read-only)
  - Fallback: KuCoin and Gate KAS/USDT tickers (public, read-only; a USD approximation)
- **Network Data**: Kaspa REST with a synced-mainnet JSON-wRPC fallback resolved through public Kaspa resolver nodes (public, read-only)
- **Resilience**: Automatic failover if primary source is rate-limited or unavailable

### Key Components
- `KaspaWidgetProvider`: 2x2 widget provider
- `KaspaWidgetLargeProvider`: 4x4 widget provider
- `WidgetUpdateWorker`: Background worker for scheduled updates
- `KaspaApiService`: API client for fetching Kaspa data
- `DataCache`: Caching layer using SharedPreferences

## Building

### Prerequisites
- Android Studio compatible with AGP 8.10.1 or later
- JDK 21 (Android 16 tests require Java 21)
- Android SDK with API 36

### Build Steps
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Build and run on device/emulator

```bash
./gradlew assembleDebug
```

## Installation

**[Download the signed Android APK](https://github.com/GEEKProtocol0110/kaspa-live-widget/releases/latest/download/kaspa-live-widget.apk)** — Android 8.0+, no GitHub account required. [Release notes and SHA256SUMS](https://github.com/GEEKProtocol0110/kaspa-live-widget/releases/latest) are available alongside the APK.

1. Download `kaspa-live-widget.apk` on your Android phone.
2. Open the APK and allow installation from your browser or file manager when Android asks.
3. Open **Kaspa Live Widget**, choose your settings, then tap **Save & add**. Or long-press the home screen → **Widgets** → add **Kaspa 2x2** or **Kaspa 4x4**.

Future signed releases update this app while keeping settings and widgets. If you used a CI debug build, the public release installs as a separate app; add its widgets before removing the old test app.

### Test the latest build on Android
1. Sign in to GitHub and open the latest successful [CI run on `main`](https://github.com/GEEKProtocol0110/kaspa-live-widget/actions/workflows/ci.yml?query=branch%3Amain).
2. Under **Artifacts**, download `debug-apk` and extract the ZIP.
3. Open the APK on your phone and allow installation from your browser or file manager when Android asks.
4. Long-press the home screen → **Widgets** → add **Kaspa 2x2** or **Kaspa 4x4**.

The CI artifact expires after seven days. This is a separate debug app and will not replace an existing release installation. Use the signed download above for everyday use, or [build from source](#building).

## Privacy & Security

- ✅ **Read-only APIs**: Only uses public APIs
- ✅ **No Wallet**: Does not store or access any wallet data
- ✅ **No Keys**: Does not handle private keys
## Documentation

- 📖 **[Quick Start Guide](QUICKSTART.md)** - Get started in minutes
- 📚 **[User Guide](USER_GUIDE.md)** - Comprehensive usage instructions
- 🏗️ **[Architecture](ARCHITECTURE.md)** - Technical architecture details
- 🎨 **[Design](DESIGN.md)** - UI/UX design specifications
- 🔨 **[Build Guide](BUILD.md)** - Detailed build instructions
- 🤝 **[Contributing](CONTRIBUTING.md)** - How to contribute

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Roadmap
- [x] Setup/settings screen with widget installation help
- [x] Dark and light appearance
- [ ] Additional widget sizes
- [x] Configurable update intervals
- [ ] Price alerts and notifications

## Support

- **Issues**: [GitHub Issues](https://github.com/GEEKProtocol0110/kaspa-live-widget/issues)
- **Discussions**: [GitHub Discussions](https://github.com/GEEKProtocol0110/kaspa-live-widget/discussions)
- **Kaspa Community**: [kaspa.org](https://kaspa.org)

## License

This project is licensed under Apache 2.0; see [LICENSE](LICENSE).

## Disclaimer

This widget is for informational purposes only. It does not provide financial advice. Always verify critical information through official sources.

---

<div align="center">

**Made with ❤️ for the Kaspa community**

[⭐ Star this repo](https://github.com/GEEKProtocol0110/kaspa-live-widget) if you find it useful!

</div>

### Neon glass layout refinement (1.2.1)

The price and DAA score stay centered over the chart when the compact widget is stretched tall. Both themes keep refresh and feed age at the bottom. USD exchange fallback first uses KuCoin 24-hour statistics (fractional change converted to percent), then Gate tickers, and finally the price-only ticker. A working price is kept when change feeds fail; missing daily change is never estimated from a different source or from chart history.

### Display polish (1.2.2)

Chart captions and refresh messages fit the compact widget without losing the full accessibility description. Positive daily changes use mint, negative changes use coral (dark) or deep red (light), and missing/zero changes remain neutral. Secondary labels are quieter, and expanded score/BPS panels have consistent separation.

## Google Play preparation

Version 1.4.2 targets Android 16/API 36 and includes a signed Android App Bundle alongside the APK. Store copy, privacy/data review notes, graphics, real-screen capture instructions and the owner submission checklist are in [play-store/](play-store/release-checklist.md). Registration, identity verification, required testing and Play submission remain owner tasks.

### X-ray and custom colors

In setup, choose **X-ray · black & white** for monochrome glass, or **Custom colors** to edit the background, text and accent using RGB sliders or `#RRGGBB`. The accent controls the chart, price, clock and Kaspa mark. Preview changes first, then tap **Apply settings** (or Save & add) to update both widget sizes. Custom colors stay saved when you switch presets. **Reset custom colors** restores the default palette; low contrast shows a readability hint.

### Personal looks (v1.5.0)

Use **Chart intensity** for a subtle line or brighter glow; 0% hides the line without inventing data. **Custom colors** includes separate clock, price, chart and network colors. **Use accent/text for all readings** links them back to the original palette. Save up to 20 named looks on your device, then load one as a draft and tap **Apply settings**. Looks leave your currency and refresh interval unchanged. Refresh feedback distinguishes queued/running work, offline or unavailable feeds, and stale saved readings.

For installation and download troubleshooting, use the [HQ widget guide](https://www.geekprotocol.xyz/tools/kaspa-widget/).

### Built-in updates (v1.6.0)

The signed direct-download Android app checks for stable releases daily while its widgets are active. A widget’s **Setup** link becomes **Update** when a newer version is saved. Open **Setup → App updates** to check manually or turn daily checks off. Tap **Update now** to download inside the app, then **Install update**. Android may first ask you to allow updates from Kaspa Live Widget and always owns installation approval. Settings and widget placement are preserved by an in-place signed upgrade.

Downloads are bounded and checked against the release SHA-256, package ID, increasing version code/name, and the installed publisher certificate. Downloads run through WorkManager and continue across screen rotation; offline or failed checks keep their last success time. Debug installations cannot replace the signed public app. Existing versions need one installation of v1.6.0 to gain this updater.

The Google Play bundle is built with `-PplayDistribution=true`; this disables direct updates and excludes the package-install permission. It uses Google Play’s own update channel after store publication. The public APK keeps the free direct-update channel.
