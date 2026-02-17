#!/bin/bash

echo "📱 INSTALL RELEASE APK"
echo "======================"

# Cari APK terbaru
LATEST_APK=$(ls -t Zashboard-*.apk 2>/dev/null | head -n1)

if [ -z "$LATEST_APK" ]; then
    echo "❌ Tidak ada APK ditemukan!"
    echo "Jalankan ./build_release.sh dulu"
    exit 1
fi

echo "APK: $LATEST_APK"
echo ""

# Cek device
DEVICES=$(adb devices | grep -v "List" | grep "device" | wc -l)
if [ "$DEVICES" -eq 0 ]; then
    echo "❌ Tidak ada device terdeteksi!"
    exit 1
fi

echo "Device terdeteksi:"
adb devices | grep -v "List"

echo ""
echo "📦 Menginstall $LATEST_APK ..."
adb install -r "$LATEST_APK"

if [ $? -eq 0 ]; then
    echo "✅ Install sukses!"
    
    # Run app
    echo ""
    echo "🚀 Menjalankan app..."
    adb shell am start -n com.udyneos.zashboard/.ui.MainActivity
    
    # Forward port
    echo ""
    echo "🔌 Forward port 9090..."
    adb forward tcp:9090 tcp:9090
    
    echo ""
    echo "🌐 Akses dashboard di: http://localhost:9090/ui/"
else
    echo "❌ Install gagal!"
fi
