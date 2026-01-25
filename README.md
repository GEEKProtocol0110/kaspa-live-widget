# Kaspa Live Widget

A free Android home screen widget that displays live Kaspa network data.

## Features

- 📱 **Two Widget Sizes**: 2x2 and 4x2 layouts
- 💰 **Live KAS Price**: Real-time USD price from CoinGecko
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

1. Build the APK or install via Android Studio
2. Long-press on home screen
3. Select "Widgets"
4. Find "Kaspa 2x2" or "Kaspa 4x2"
5. Drag to home screen

## Privacy & Security

- ✅ **Read-only APIs**: Only uses public APIs
- ✅ **No Wallet**: Does not store or access any wallet data
- ✅ **No Keys**: Does not handle private keys
- ✅ **No Backend**: Direct API calls only
- ✅ **No Personal Data**: Does not collect user information

## License

See LICENSE file for details.