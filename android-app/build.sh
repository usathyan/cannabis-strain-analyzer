#!/bin/bash

# 🌿 Cannabis Strain Analyzer - Android Build Script
# This script builds the Android app and generates a sideloadable APK

set -e  # Exit on any error

echo "🌿 Cannabis Strain Analyzer - Android Build Script"
echo "=================================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if we're in the correct directory
if [ ! -f "settings.gradle.kts" ]; then
    echo -e "${RED}Error: Must run this script from the android-app directory${NC}"
    exit 1
fi

echo -e "${YELLOW}Step 1: Checking prerequisites...${NC}"

# Check for Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed${NC}"
    echo "Please install JDK 17 or higher"
    exit 1
fi

echo -e "${GREEN}✓ Java found: $(java -version 2>&1 | head -n 1)${NC}"

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo -e "${YELLOW}Warning: ANDROID_HOME not set${NC}"
    echo "Using gradlew bundled SDK"
fi

echo ""
echo -e "${YELLOW}Step 2: Cleaning previous builds...${NC}"
./gradlew clean

echo ""
echo -e "${YELLOW}Step 3: Building debug APK...${NC}"
./gradlew assembleDebug

# Check if build was successful
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo ""
    echo -e "${GREEN}✓ Build successful!${NC}"
    echo ""
    echo "📦 APK Location:"
    echo "   $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "📱 Installation Instructions:"
    echo ""
    echo "Via ADB (device connected via USB):"
    echo "   adb install app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "Via File Transfer:"
    echo "   1. Copy app-debug.apk to your device"
    echo "   2. Open file manager and tap the APK"
    echo "   3. Allow installation from unknown sources"
    echo "   4. Install"
    echo ""
    echo "⚠️  IMPORTANT:"
    echo "   - Ensure backend server is running: http://localhost:8000"
    echo "   - For physical devices, update BASE_URL in ApiClient.kt"
    echo "   - Device must be on same WiFi network"
    echo ""
    
    # Get APK size
    APK_SIZE=$(du -h "app/build/outputs/apk/debug/app-debug.apk" | cut -f1)
    echo -e "${GREEN}APK Size: $APK_SIZE${NC}"
    echo ""
else
    echo -e "${RED}✗ Build failed!${NC}"
    echo "Check the error messages above"
    exit 1
fi

echo -e "${GREEN}Build process completed!${NC}"
echo ""

# Ask if user wants to install on connected device
if command -v adb &> /dev/null; then
    DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)
    if [ "$DEVICES" -gt 0 ]; then
        echo -e "${YELLOW}Found $DEVICES connected device(s)${NC}"
        read -p "Do you want to install the app now? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "Installing..."
            adb install -r app/build/outputs/apk/debug/app-debug.apk
            echo -e "${GREEN}✓ Installation complete!${NC}"
        fi
    fi
fi
