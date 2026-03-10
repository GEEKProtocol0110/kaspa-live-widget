# Quick Start Guide - Kaspa Live Widget

Get your Kaspa Live Widget up and running in minutes!

## For Users (Installing the Widget)

### Prerequisites
- Android device running Android 8.0 (API 26) or higher
- Internet connection

### Installation Steps

1. **Download the APK**
   - Get the latest release APK from the GitHub releases page
   - Or build from source (see below)

2. **Install on Your Device**
   ```
   Settings → Security → Unknown Sources → Enable
   ```
   - Tap the downloaded APK file
   - Follow installation prompts
   - Grant required permissions (Internet access)

3. **Add Widget to Home Screen**
   - Long-press on empty area of home screen
   - Tap "Widgets"
   - Scroll to find "Kaspa 2x2" or "Kaspa 4x2"
   - Long-press and drag to desired location
   - Release to place

4. **Wait for First Update**
   - Initial display shows "--" for data
   - First update occurs within 15 minutes
   - Subsequent updates every 15 minutes

### What You'll See

**2x2 Widget (Compact)**
- Current time
- KAS/USD price
- Block height

**4x2 Widget (Extended)**
- Current time
- KAS/USD price
- Block height
- Blocks per second (BPS)
- Network hashrate

---

## For Developers (Building from Source)

### Prerequisites
- **Android Studio** Arctic Fox or later
- **JDK** 8 or higher (JDK 11+ recommended)
- **Android SDK** with API 34

### Quick Build

```bash
# 1. Clone the repository
git clone https://github.com/GEEKProtocol0110/kaspa-live-widget.git
cd kaspa-live-widget

# 2. Build using Gradle wrapper
./gradlew assembleDebug

# 3. Find APK at:
# app/build/outputs/apk/debug/app-debug.apk

# 4. Install on connected device
./gradlew installDebug
```

### Using Android Studio

```
1. File → Open → Select project directory
2. Wait for Gradle sync
3. Run → Run 'app' (or press Shift+F10)
4. Select device/emulator
```

### Building Release APK

```bash
# Generate keystore (first time only)
keytool -genkey -v -keystore kaspa-widget.keystore \
  -alias kaspa-widget -keyalg RSA -keysize 2048 -validity 10000

# Build release
./gradlew assembleRelease

# APK location:
# app/build/outputs/apk/release/app-release.apk
```

---

## Testing the Widget

### First Time Setup

1. **Verify Installation**
   ```
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Add to Home Screen**
   - See user installation steps above

3. **Check Logs** (if issues)
   ```
   adb logcat | grep Kaspa
   ```

### Testing Updates

1. **Trigger Manual Update** (via ADB)
   ```bash
   adb shell am broadcast \
     -a android.appwidget.action.APPWIDGET_UPDATE \
     -n com.kaspa.livewidget/.widget.KaspaWidgetProvider
   ```

2. **Force WorkManager Task**
   ```bash
   adb shell cmd jobscheduler run -f com.kaspa.livewidget 1
   ```

3. **Clear Cache** (test fresh install)
   ```bash
   adb shell pm clear com.kaspa.livewidget
   ```

### Verifying Data

1. **Check API Endpoints**
   ```bash
   # Price (primary)
   curl "https://kaspa.stream/api/v1/price"

   # Price (failsafe fallback)
   curl "https://api.coingecko.com/api/v3/simple/price?ids=kaspa&vs_currencies=usd"
   
   # Network Info
   curl "https://api.kaspa.org/info/blockdag"
   
   # Hashrate
   curl "https://api.kaspa.org/info/hashrate"
   ```

2. **Inspect SharedPreferences**
   ```bash
   adb shell run-as com.kaspa.livewidget \
     cat /data/data/com.kaspa.livewidget/shared_prefs/kaspa_widget_cache.xml
   ```

---

## Troubleshooting

### Widget Not Showing Data

**Symptom**: Widget displays "--" or "N/A"

**Solutions**:
1. Check internet connection
2. Wait 15 minutes for first update
3. Remove and re-add widget
4. Check device time is correct
5. Verify APIs are accessible

### Widget Not Updating

**Symptom**: Data is stale (> 15 minutes old)

**Solutions**:
1. Check battery optimization settings
   ```
   Settings → Battery → Battery Optimization
   → All Apps → Kaspa Live Widget → Don't Optimize
   ```
2. Verify WorkManager is running
   ```bash
   adb shell dumpsys jobscheduler | grep kaspa
   ```
3. Check app permissions (Internet)
4. Restart device

### Build Errors

**Symptom**: Gradle sync or build fails

**Solutions**:
1. **Sync issues**
   ```
   File → Invalidate Caches / Restart
   ```

2. **Dependency issues**
   ```bash
   ./gradlew --refresh-dependencies
   ```

3. **SDK not found**
   ```
   File → Project Structure → SDK Location
   → Set Android SDK path
   ```

4. **Out of memory**
   - Add to gradle.properties:
   ```properties
   org.gradle.jvmargs=-Xmx4096m
   ```

### APK Installation Fails

**Symptom**: "App not installed" error

**Solutions**:
1. Enable unknown sources
2. Uninstall previous version
3. Check APK signature (if updating)
4. Verify device has sufficient storage

---

## Performance Monitoring

### Check Battery Usage

```bash
# Battery stats
adb shell dumpsys batterystats com.kaspa.livewidget

# Wake locks
adb shell dumpsys power | grep -i kaspa
```

### Network Usage

```bash
# Network stats (requires root)
adb shell dumpsys netstats detail full | grep kaspa
```

### Memory Usage

```bash
# Memory info
adb shell dumpsys meminfo com.kaspa.livewidget
```

---

## Development Tips

### Live Debugging

```bash
# View real-time logs
adb logcat -c && adb logcat | grep -i "kaspa\|widget"

# Debug widget updates
adb logcat | grep -i "AppWidgetManager"

# Debug WorkManager
adb logcat | grep -i "WorkManager"
```

### Quick Iteration

```bash
# Build and install in one command
./gradlew installDebug && \
adb shell am broadcast \
  -a android.appwidget.action.APPWIDGET_UPDATE \
  -n com.kaspa.livewidget/.widget.KaspaWidgetProvider
```

### Testing on Multiple Devices

```bash
# List connected devices
adb devices

# Install on specific device
adb -s <device_id> install -r app-debug.apk
```

---

## Next Steps

### After Installation
1. ✅ Add widget to home screen
2. ✅ Wait for first data update
3. ✅ Customize placement and size
4. 📱 Add second widget (4x2 if you have 2x2)
5. ⭐ Star the repository on GitHub
6. 🐛 Report issues or suggest features

### For Contributors
1. 📖 Read [CONTRIBUTING.md](CONTRIBUTING.md)
2. 🏗️ Review [ARCHITECTURE.md](ARCHITECTURE.md)
3. 🎨 Check [DESIGN.md](DESIGN.md)
4. 💻 Make your first contribution!

---

## Resources

### Documentation
- [README.md](README.md) - Project overview
- [BUILD.md](BUILD.md) - Detailed build instructions
- [USER_GUIDE.md](USER_GUIDE.md) - End-user guide
- [ARCHITECTURE.md](ARCHITECTURE.md) - Technical architecture
- [DESIGN.md](DESIGN.md) - Widget design specs
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines

### APIs
- [kaspa.stream](https://kaspa.stream/)
- [CoinGecko API (failsafe)](https://www.coingecko.com/en/api)
- [Kaspa Network API](https://api.kaspa.org/)

### Android Development
- [Android Widgets Guide](https://developer.android.com/develop/ui/views/appwidgets/overview)
- [WorkManager Guide](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

### Community
- [Kaspa Discord](https://discord.gg/kaspa)
- [Kaspa Reddit](https://reddit.com/r/kaspa)
- [GitHub Issues](https://github.com/GEEKProtocol0110/kaspa-live-widget/issues)

---

## Support

Need help? Have questions?

1. 📖 Check the documentation above
2. 🔍 Search existing [GitHub Issues](https://github.com/GEEKProtocol0110/kaspa-live-widget/issues)
3. 💬 Open a new issue
4. 📧 Contact the maintainer

---

**Happy Widget-ing! 🚀**
