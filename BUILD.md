# Build Instructions

## Prerequisites

- **Android Studio**: compatible with AGP 8.10.1 or later
- **JDK**: 21 (required for Android 16 Robolectric tests)
- **Android SDK**: 
  - Compile SDK: API 36 (Android 16)
  - Min SDK: API 26 (Android 8.0)
  - Build Tools: 35.0.0

## Environment Setup

### 1. Install Android Studio
Download from: https://developer.android.com/studio

### 2. Configure SDK
In Android Studio:
1. Go to `Tools > SDK Manager`
2. Install:
   - Android SDK Platform 36
   - Android SDK Build-Tools 35.0.0
   - Android SDK Platform-Tools

### 3. Set ANDROID_HOME (Optional)
Add to your `.bashrc` or `.zshrc`:
```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/tools
```

## Building the Project

### Using Android Studio (Recommended)

1. **Clone the repository**:
   ```bash
   git clone https://github.com/GEEKProtocol0110/kaspa-live-widget.git
   cd kaspa-live-widget
   ```

2. **Open in Android Studio**:
   - Launch Android Studio
   - Click "Open an Existing Project"
   - Navigate to the cloned directory
   - Click "OK"

3. **Sync Gradle**:
   - Android Studio will automatically prompt to sync
   - Or click "File > Sync Project with Gradle Files"

4. **Build the APK**:
   - Click "Build > Build Bundle(s) / APK(s) > Build APK(s)"
   - Or use the toolbar button
   - APK will be generated in `app/build/outputs/apk/debug/`

### Using Command Line

1. **Clone the repository**:
   ```bash
   git clone https://github.com/GEEKProtocol0110/kaspa-live-widget.git
   cd kaspa-live-widget
   ```

2. **Make gradlew executable** (Linux/Mac):
   ```bash
   chmod +x gradlew
   ```

3. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   
   On Windows:
   ```cmd
   gradlew.bat assembleDebug
   ```

4. **Find the APK**:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

## Building Release APK

Use the persistent key setup in [SIGNED_RELEASES.md](SIGNED_RELEASES.md). Gradle reads `WIDGET_KEYSTORE_PATH`, `WIDGET_STORE_PASSWORD`, `WIDGET_KEY_ALIAS` and `WIDGET_KEY_PASSWORD` from the environment; it does not read a plaintext properties file. All four are required together. With none set, development/CI release builds are unsigned and cannot serve as public installation updates.

```bash
./gradlew build
./gradlew assembleRelease
```

APKs are under `app/build/outputs/apk/release/`. Public tagged releases require the owner’s signing configuration and publish the stable asset `kaspa-live-widget.apk`. Keep the same key and increase the version code for every update.

## Running on Device/Emulator

### Using Android Studio

1. **Connect Device** or **Start Emulator**
2. **Select device** from dropdown
3. Click **Run** (green play button)

### Using ADB

1. **Enable USB Debugging** on your device
2. **Connect device** via USB
3. **Install APK**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Troubleshooting

### Gradle Sync Failed
- Check internet connection
- Update Gradle version in `gradle/wrapper/gradle-wrapper.properties`
- Invalidate caches: `File > Invalidate Caches / Restart`

### Build Failed
- Clean project: `Build > Clean Project`
- Rebuild: `Build > Rebuild Project`
- Check JDK version: `File > Project Structure > SDK Location`

### SDK Not Found
- Open SDK Manager and install required components
- Set ANDROID_HOME environment variable

### Out of Memory
Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=1024m
```

## Testing the Widget

After installation:
1. Long-press on home screen
2. Tap "Widgets"
3. Find "Kaspa 2x2" or "Kaspa 4x4"
4. Add to home screen
5. Widget requests a refresh immediately; tap it to see refresh feedback

## Dependencies

The project uses:
- AndroidX Core KTX 1.12.0
- AndroidX AppCompat 1.6.1
- WorkManager 2.9.0
- OkHttp 4.11.0
- Gson 2.10.1
- Kotlin Coroutines 1.7.3

All dependencies are specified in `app/build.gradle.kts`.

## Support

For build issues, please check:
- Android Studio version compatibility
- JDK version
- SDK installations
- Internet connectivity for dependency downloads

If problems persist, open an issue on GitHub.
