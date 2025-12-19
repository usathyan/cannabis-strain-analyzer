# 🌿 Cannabis Strain Analyzer - Android App

A modern Android application for cannabis strain analysis and comparison, built with Kotlin and Jetpack Compose.

## 📱 Features

- **Home Dashboard**: View your profile statistics, quick actions, and recent analyses
- **Configure Profile**: Select favorite strains and create your ideal cannabis profile
- **Compare Strains**: Analyze and compare strains against your preferences
- **Z-Score Analysis**: Advanced similarity matching using z-scored cosine similarity
- **Real-time Backend Integration**: Connects to the FastAPI backend server
- **Material Design 3**: Modern UI with Material You theming

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with StateFlow
- **Networking**: Retrofit + OkHttp
- **JSON Parsing**: Gson
- **Navigation**: Jetpack Navigation Compose
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## 📋 Prerequisites

Before building the app, ensure you have:

1. **Android Studio** (Hedgehog or later)
   - Download from: https://developer.android.com/studio

2. **Java Development Kit (JDK) 17 or higher**
   - Check version: `java -version`
   - Download from: https://adoptium.net/

3. **Backend Server Running**
   - The FastAPI backend must be running on your local machine or accessible network
   - Default backend URL: `http://localhost:8000`

## 🚀 Setup Instructions

### 1. Backend Server Setup

First, ensure your FastAPI backend is running:

```bash
# Navigate to the main project directory
cd /Users/umeshbhatt/code/weed

# Start the backend server
make run
```

The server should be accessible at `http://localhost:8000`

### 2. Configure Network Settings

#### For Android Emulator:
The app is pre-configured to use `http://10.0.2.2:8000/` which maps to localhost on your host machine.

#### For Physical Device:
You need to update the API endpoint:

1. Open `app/src/main/kotlin/com/strainanalyzer/app/network/ApiClient.kt`
2. Change the `BASE_URL` to your computer's IP address:

```kotlin
private const val BASE_URL = "http://YOUR_COMPUTER_IP:8000/"
```

To find your computer's IP:
- **Mac**: `ifconfig | grep "inet " | grep -v 127.0.0.1`
- **Linux**: `ip addr show | grep "inet " | grep -v 127.0.0.1`
- **Windows**: `ipconfig` (look for IPv4 Address)

Example: `http://192.168.1.100:8000/`

**Important**: Your device must be on the same WiFi network as your computer.

### 3. Build the App

#### Option A: Using Android Studio (Recommended)

1. Open Android Studio
2. Click "Open" and select the `android-app` folder
3. Wait for Gradle sync to complete
4. Click the "Run" button (green play icon) or press `Shift + F10`
5. Select your device/emulator
6. The app will build and install automatically

#### Option B: Using Command Line

```bash
# Navigate to the android-app directory
cd /Users/umeshbhatt/code/weed/android-app

# Build debug APK
./gradlew assembleDebug

# The APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk
```

### 4. Install APK on Device

#### Via USB (ADB):
```bash
# Enable USB debugging on your Android device
# Connect device via USB
# Run:
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### Via File Transfer:
1. Copy `app-debug.apk` to your device
2. Open file manager on device
3. Tap the APK file
4. Allow installation from unknown sources if prompted
5. Install the app

## 📦 Building Release APK

To build a production-ready APK:

```bash
# Build release APK (unsigned)
./gradlew assembleRelease

# APK location: app/build/outputs/apk/release/app-release-unsigned.apk
```

For signed releases, you'll need to:
1. Generate a keystore
2. Configure signing in `app/build.gradle.kts`
3. Build with: `./gradlew assembleRelease`

## 🎨 App Screens

### 1. Home Screen
- Profile statistics (strains, THC%, analyses)
- Quick action buttons
- Recent analyses list

### 2. Configure Screen
- View favorite strains
- Select from available strains
- Update ideal cannabis profile
- View current profile statistics

### 3. Compare Screen
- Search for strains
- Toggle Z-Score analysis
- View detailed comparison results
- See terpene profiles and AI analysis

### 4. Profile Screen
- User information
- Profile statistics

## 🔧 Troubleshooting

### Connection Issues

**Problem**: Cannot connect to backend
**Solutions**:
- Verify backend is running: `curl http://localhost:8000/api/available-strains`
- Check firewall settings
- Ensure correct IP address (for physical device)
- Verify device is on same network

### Build Issues

**Problem**: Gradle sync fails
**Solutions**:
- File > Invalidate Caches / Restart
- Delete `.gradle` folder and rebuild
- Update Android Studio to latest version

**Problem**: Missing dependencies
**Solutions**:
- Check internet connection
- Sync project with Gradle files
- Update repositories in `build.gradle.kts`

### Runtime Issues

**Problem**: App crashes on launch
**Solutions**:
- Check Logcat for error messages
- Verify network permissions in AndroidManifest.xml
- Ensure backend server is accessible

## 📱 Testing

### Using Android Emulator
1. Open AVD Manager in Android Studio
2. Create a new virtual device (Pixel 5 recommended)
3. Select system image (Android 10+ recommended)
4. Launch emulator
5. Run the app

### Using Physical Device
1. Enable Developer Options on your device:
   - Settings > About Phone > Tap "Build Number" 7 times
2. Enable USB Debugging:
   - Settings > Developer Options > USB Debugging
3. Connect via USB
4. Run the app from Android Studio

## 🏗️ Project Structure

```
android-app/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/com/strainanalyzer/app/
│   │       │   ├── MainActivity.kt          # Main entry point
│   │       │   ├── data/
│   │       │   │   └── Models.kt            # Data models
│   │       │   ├── network/
│   │       │   │   ├── ApiService.kt        # Retrofit API interface
│   │       │   │   └── ApiClient.kt         # Network client
│   │       │   └── ui/
│   │       │       ├── StrainViewModel.kt   # State management
│   │       │       ├── HomeScreen.kt        # Home UI
│   │       │       ├── ConfigureScreen.kt   # Configuration UI
│   │       │       ├── CompareScreen.kt     # Comparison UI
│   │       │       └── theme/               # Material Design theme
│   │       ├── res/                         # Resources (strings, themes)
│   │       └── AndroidManifest.xml          # App manifest
│   └── build.gradle.kts                     # App-level Gradle config
├── build.gradle.kts                         # Project-level Gradle config
├── settings.gradle.kts                      # Gradle settings
└── README.md                                # This file
```

## 🔐 Permissions

The app requires the following permissions:

- `INTERNET`: To communicate with the backend API
- `ACCESS_NETWORK_STATE`: To check network connectivity

These are automatically granted and don't require user approval.

## 📊 API Endpoints Used

- `GET /api/available-strains` - Fetch available strains
- `GET /api/user-profile` - Get user profile data
- `POST /api/create-ideal-profile` - Create ideal profile
- `POST /api/add-strain-to-profile` - Add strain to favorites
- `POST /api/remove-strain-from-profile` - Remove strain
- `POST /api/compare-strain` - Compare strain analysis
- `GET /api/ideal-profile` - Get ideal profile details

## 🎯 Future Enhancements

- [ ] Offline mode with local database
- [ ] Push notifications for new strain recommendations
- [ ] Dark mode theme
- [ ] Share results with friends
- [ ] Strain favorites bookmarking
- [ ] History of comparisons
- [ ] Advanced filtering and sorting
- [ ] Barcode scanner for strain lookup

## 🐛 Known Issues

1. **First Launch**: Initial profile load may take a few seconds
2. **Network Timeout**: Long analysis requests may timeout on slow connections
3. **Large Datasets**: Loading 100+ strains may cause lag on older devices

## 📄 License

This project is licensed under the MIT License - see the LICENSE file in the root directory.

## 🙏 Acknowledgments

- Backend API: FastAPI Cannabis Strain Analyzer
- UI Design: Material Design 3 Guidelines
- Icons: Material Icons

## 📞 Support

For issues or questions:
1. Check this README first
2. Review the main project's README
3. Check Android Studio Logcat for error messages
4. Ensure backend server is running and accessible

---

**Note**: This app is for educational and research purposes only. Always consult with healthcare professionals regarding cannabis use.
