# Build Instructions

## Prerequisites

- **Android Studio**: Arctic Fox (2020.3.1) or later
- **JDK**: 8 or later (JDK 11+ recommended)
- **Android SDK**: 
  - Compile SDK: API 34 (Android 14)
  - Min SDK: API 26 (Android 8.0)
  - Build Tools: 34.0.0

## Environment Setup

### 1. Install Android Studio
Download from: https://developer.android.com/studio

### 2. Configure SDK
In Android Studio:
1. Go to `Tools > SDK Manager`
2. Install:
   - Android SDK Platform 34
   - Android SDK Build-Tools 34.0.0
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

### 1. Create Keystore (First Time Only)

```bash
keytool -genkey -v -keystore kaspa-widget.keystore -alias kaspa-widget -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Sign the APK

Create `keystore.properties` in project root:
```properties
storeFile=/path/to/kaspa-widget.keystore
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=kaspa-widget
keyPassword=YOUR_KEY_PASSWORD
```

**Important**: Never commit `keystore.properties` to Git!

### 3. Build Release

Release signing is not currently wired into the Gradle build. Store your keystore and passwords securely and add a release signing configuration before shipping; `assembleRelease` alone may produce an unsigned APK that cannot be installed.

```bash
./gradlew assembleRelease
```

APK will be in: `app/build/outputs/apk/release/app-release.apk`

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
3. Find "Kaspa 2x2" or "Kaspa 4x2"
4. Add to home screen
5. Widget should fetch data within 15 minutes

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
