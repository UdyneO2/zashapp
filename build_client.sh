#!/bin/bash

echo "🚀 BUILDING ZASHBOARD CLIENT"
echo "============================="

# Clean
./gradlew clean

# Build debug APK
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo "📱 APK location: app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "📦 Size: $(ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')"
    echo ""
    echo "🚀 Install with: adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo "🌐 After install, connect to any Zashboard server at port 9090"
else
    echo "❌ Build failed"
    exit 1
fi
