#!/usr/bin/env bash
set -e

ADB="/home/gjgameryt-0890/Android/Sdk/platform-tools/adb"
APK="/home/gjgameryt-0890/PocketSparrow/pocket-sparrow-mobile.apk"

echo "Waiting for device to connect via ADB..."
$ADB wait-for-device

DEVICE=$($ADB devices | grep -w "device" | head -n 1 | awk '{print $1}')
echo "Device detected: $DEVICE"

echo "Installing $APK..."
$ADB -s "$DEVICE" install -r "$APK"

echo "Launching MainActivity..."
$ADB -s "$DEVICE" shell am start -n com.pocketsparrow.pocket_sparrow/.MainActivity

echo "Sync completed successfully!"
