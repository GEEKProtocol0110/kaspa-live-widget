<div align="center">

# Kaspa Live Widget

[![License](https://img.shields.io/github/license/GEEKProtocol0110/kaspa-live-widget)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8-blue.svg)](https://kotlinlang.org)
[![Release](https://img.shields.io/github/v/release/GEEKProtocol0110/kaspa-live-widget)](https://github.com/GEEKProtocol0110/kaspa-live-widget/releases)

**A free, open-source Android home screen widget that displays live Kaspa network data.**

[Features](#features) • [Installation](#installation) • [Building](#building) • [Documentation](#documentation) • [Contributing](#contributing)

</div>

---

## Features

- 📱 **Two Widget Sizes**: 2x2 and 4x2 layouts
- 💰 **Live KAS Price**: Auto-updated USD price from CoinGecko
- 📊 **Network Stats**: Block height, BPS (Blocks Per Second), and hashrate
- ⏰ **Current Time**: Shows current time on widget
- 🔄 **Auto Updates**: Scheduled updates every 15 minutes using WorkManager
- 💾 **Smart Caching**: Caches data to reduce API calls
- 🎨 **Clean Design**: Modern dark theme with Kaspa brand colors

## Widget Layouts

### 2x2 Widget (Compact)
Displays:
- Current time
- KAS price (USD)
- Block height

### 4x2 Widget (Extended)
Displays:
- Current time
- KAS price (USD)
- Block height
- BPS (Blocks Per Second)
- Network hashrate

## Technical Details

### Architecture
- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Update Mechanism**: AppWidgetProvider + WorkManager
- **Update Interval**: 15 minutes

### Data Sources
- **Price Data**: Multiple sources with automatic fallback
  - Primary: CoinGecko API (public, read-only)
  - Fallback: Kaspa exchange APIs (public, read-only)
- **Network Data**: Kaspa API (public, read-only)
- **Resilience**: Automatic failover if primary source is rate-limited or unavailable

### Key Components
- `KaspaWidgetProvider`: 2x2 widget provider
- `KaspaWidgetLargeProvider`: 4x2 widget provider
- `WidgetUpdateWorker`: Background worker for scheduled updates
- `KaspaApiService`: API client for fetching Kaspa data
- `DataCache`: Caching layer using SharedPreferences

## Building

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 8 or later
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

### Option 1: Download Release APK (Recommended)
1. Go to [Releases](https://github.com/GEEKProtocol0110/kaspa-live-widget/releases)
2. Download the latest `kaspa-widget-release.apk`
3. Install on your Android device
4. Long-press on home screen → Widgets → Select "Kaspa 2x2" or "Kaspa 4x2"

### Option 2: Build from Source
See [Building](#building) section below

## Privacy & Security

- ✅ **Read-only APIs**: Only uses public APIs
- ✅ **No Wallet**: Does not store or access any wallet data
- ✅ **No Keys**: Does not handle private keys
- ✅Documentation

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

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Disclaimer

This widget is for informational purposes only. It does not provide financial advice. Always verify critical information through official sources.

---

<div align="center">

**Made with ❤️ for the Kaspa community**

[⭐ Star this repo](https://github.com/GEEKProtocol0110/kaspa-live-widget) if you find it useful!

</div>
## License

See LICENSE file for details.