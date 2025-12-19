# 🚀 Quick Start - Build Your Android App in 5 Minutes

## Step 1: Install Android Studio (One Time Only)

**Download**: https://developer.android.com/studio

- Choose your OS (Mac/Windows/Linux)
- Install and launch
- Complete setup wizard
- No need to create a new project

**Time**: ~10 minutes (including download)

## Step 2: Start the Backend

```bash
cd /Users/umeshbhatt/code/weed
make run
```

Verify it's running:
```bash
curl http://localhost:8000/api/available-strains
```

You should see JSON with strain data.

## Step 3: Open Project in Android Studio

1. Launch Android Studio
2. Click **"Open"**
3. Navigate to: `/Users/umeshbhatt/code/weed/android-app`
4. Click **"Open"**
5. Wait for "Gradle sync" to finish (~5 minutes first time)
   - You'll see progress in the bottom status bar
   - First sync downloads all Android dependencies

## Step 4: Build the APK

Once Gradle sync completes:

1. Click **Build** in the top menu
2. Select **Build Bundle(s) / APK(s)**
3. Click **Build APK(s)**
4. Wait ~2-3 minutes
5. Click **"locate"** in the success notification

**APK Location**: `app/build/outputs/apk/debug/app-debug.apk`

## Step 5: Install on Device

### Using Android Emulator (Easiest!)

1. In Android Studio, click **Tools → AVD Manager**
2. Click **"Create Virtual Device"**
3. Select **Pixel 5** → Click **Next**
4. Select **R** (API 30) → Click **Next** → **Finish**
5. Click the **green play button** next to your device
6. Wait for emulator to boot (~1-2 minutes)
7. Drag and drop the APK onto the emulator
8. App installs and launches automatically!

### Using Your Phone

1. Enable Developer Mode:
   - Settings → About Phone
   - Tap "Build Number" 7 times

2. Enable USB Debugging:
   - Settings → Developer Options
   - Turn on "USB Debugging"

3. Connect phone via USB cable

4. Run in Terminal:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

**Note**: For physical device, update the API URL first (see below)

## ⚙️ Configuration for Physical Device

If using a real phone (not emulator):

1. Find your computer's IP address:
   ```bash
   ifconfig | grep "inet " | grep -v 127.0.0.1
   ```
   Example output: `192.168.1.100`

2. Edit this file in Android Studio:
   `app/src/main/kotlin/com/strainanalyzer/app/network/ApiClient.kt`

3. Change line 14 from:
   ```kotlin
   private const val BASE_URL = "http://10.0.2.2:8000/"
   ```
   To (use YOUR IP):
   ```kotlin
   private const val BASE_URL = "http://192.168.1.100:8000/"
   ```

4. Rebuild: Build → Build Bundle(s) / APK(s) → Build APK(s)

5. Ensure phone and computer are on same WiFi

## ✅ Verify It's Working

After installation:

1. **App launches** → Shows home screen with green header
2. **Tap Configure** → Shows available strains grid
3. **Select a strain** → Border turns green
4. **Tap Compare** → Shows search bar
5. **Enter "Blue Dream"** → Search works
6. **Tap Analyze** → Shows results (if backend is running)

If any step fails:
- Check backend is running: `curl http://localhost:8000/api/available-strains`
- For emulator: No extra config needed
- For phone: Verify IP address is correct and on same WiFi

## 🎉 That's It!

You now have:
- ✅ Working Android app
- ✅ Sideloadable APK
- ✅ Running on device/emulator

## 📱 Share Your App

To share with others:

1. Copy the APK:
   ```bash
   cp app/build/outputs/apk/debug/app-debug.apk ~/Desktop/StrainAnalyzer.apk
   ```

2. Send via:
   - Email attachment
   - Cloud storage (Google Drive, Dropbox)
   - USB transfer
   - Messaging apps

3. Recipient installs:
   - Download APK on Android device
   - Tap to install
   - Allow "Install from unknown sources"

**Remember**: They'll need access to your backend server or you'll need to deploy it to a public URL!

## 🆘 Need Help?

- **Gradle sync fails** → Restart Android Studio
- **Can't find APK** → Check `app/build/outputs/apk/debug/`
- **Connection errors** → Backend not running or wrong IP
- **App crashes** → Check Logcat in Android Studio

For detailed help, see:
- `README.md` - Complete documentation
- `ANDROID_BUILD_GUIDE.md` - Troubleshooting guide
- `ANDROID_APP_SUMMARY.md` - Feature overview

---

**Total Time**: ~20 minutes (including downloads and first-time setup)

**Next Time**: Just steps 2, 4, and 5 (~5 minutes)
