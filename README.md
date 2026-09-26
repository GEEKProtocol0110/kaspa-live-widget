<div align="center">

# Kaspa Live Widget

[![License](https://img.shields.io/github/license/GEEKProtocol0110/kaspa-live-widget)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8-blue.svg)](https://kotlinlang.org)
[![Android CI](https://github.com/GEEKProtocol0110/kaspa-live-widget/actions/workflows/ci.yml/badge.svg)](https://github.com/GEEKProtocol0110/kaspa-live-widget/actions/workflows/ci.yml)

**A free, open-source Android home screen widget that displays live Kaspa network data.**

[Features](#features) • [Installation](#installation) • [Building](#building) • [Documentation](#documentation) • [Contributing](#contributing)

</div>

---

## Features

- 📱 **Two Widget Sizes**: 2x2 (quick glance) and 4x4 (expanded view)
- 🎨 **Unified Design**: Kaspa mint background with a mirrored black K on both sizes and the app icon
- 💰 **Live KAS Price**: Auto-updated USD price from kaspa.stream with multi-source fallback
- 📊 **Network Stats**: Block height, observed average BPS (after two readings), and hashrate
- ⏰ **Current Time**: Shows current time on widget
- 🔄 **Auto Updates**: Scheduled updates every 15 minutes using WorkManager
- 💾 **Honest Freshness**: Keeps the last good reading and marks it stale when updates stop
- 🎨 **Clean Design**: Dark translucent data panels over Kaspa brand visuals

## Widget Layouts

### 2x2 Widget (Quick Glance)
**Purpose**: Minimal information at a glance
- Kaspa mint background with a centered mirrored K
- Current time (top)
- KAS price (center focus, in dark panel)
- Block height (bottom panel)
- No clutter, maximum clarity

### 4x4 Widget (Command View)
**Purpose**: Expanded view with more breathing room
- Same Kaspa mint background and mirrored K
- Time + "Last updated" indicator (top row)
- Expanded KAS price (center); the 24h change field is currently a placeholder
- Block height + BPS (middle row, side-by-side panels)
- Network hashrate (bottom panel)
- More space, not more noise

## Technical Details

### Architecture
- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Update Mechanism**: AppWidgetProvider + WorkManager
- **Update Interval**: 15 minutes

### Data Sources
- **Price Data**: Multiple sources with automatic fallback
  - Primary: kaspa.stream (public, read-only)
  - Secondary fallback: Kaspa exchange APIs (public, read-only)
  - Failsafe fallback: CoinGecko API (public, read-only)
- **Network Data**: Kaspa API (public, read-only)
- **Resilience**: Automatic failover if primary source is rate-limited or unavailable

### Key Components
- `KaspaWidgetProvider`: 2x2 widget provider
- `KaspaWidgetLargeProvider`: 4x4 widget provider
- `WidgetUpdateWorker`: Background worker for scheduled updates
- `KaspaApiService`: API client for fetching Kaspa data
- `DataCache`: Caching layer using SharedPreferences

## Building

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK with API 34

### Build Steps
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Build and run on device/emulator

```bash
./gradlew assembleDebug
```

## Installation

**No signed public release has been published yet.** The green CI badge means the source builds; it is not an app download.

### Test the latest build on Android
1. Sign in to GitHub and open the latest successful [CI run on `main`](https://github.com/GEEKProtocol0110/kaspa-live-widget/actions/workflows/ci.yml?query=branch%3Amain).
2. Under **Artifacts**, download `debug-apk` and extract the ZIP.
3. Open the APK on your phone and allow installation from your browser or file manager when Android asks.
4. Long-press the home screen → **Widgets** → add **Kaspa 2x2** or **Kaspa 4x4**.

The CI artifact expires after seven days. This is a separate debug app and will not replace an existing release installation. For a lasting build, [build from source](#building). A signed release will be linked from [Releases](https://github.com/GEEKProtocol0110/kaspa-live-widget/releases) when one exists.

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
- [ ] Settings screen for customization
- [ ] Multiple theme options
- [ ] Additional widget sizes
- [ ] Configurable update intervals
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
