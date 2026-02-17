# Udyneos Zashboard untuk Ubuntu

Aplikasi Android yang menjalankan Zashboard dashboard di localhost:9090/ui

## 📋 Requirements Ubuntu

- Ubuntu 20.04 / 22.04 / 24.04
- Java 17 (OpenJDK)
- Android SDK
- Gradle 8.2+

## 🚀 Quick Start di Ubuntu

```bash
# 1. Clone atau extract project
cd ~/Projects/zashboard-android

# 2. Download Zashboard files
chmod +x setup_zashboard.sh
./setup_zashboard.sh

# 3. Build APK
./gradlew assembleDebug

# 3. Build APK
./gradlew assembleDebug

# 4. Install ke device/emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

📦 Build Release di Ubuntu

```bash
# Generate keystore (jika belum punya)
keytool -genkey -v -keystore release.keystore -alias udyneos -keyalg RSA -keysize 2048 -validity 10000

# Build release APK
./gradlew assembleRelease

# Release APK location:
# app/build/outputs/apk/release/app-release.apk
```

🔧 Struktur Project

```
zashboard-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/udyneos/zashboard/
│   │   │   ├── server/      # Server implementation
│   │   │   ├── ui/          # UI components
│   │   │   ├── utils/       # Utilities
│   │   │   └── models/      # Data models
│   │   └── assets/
│   │       └── zashboard/   # Zashboard web files
│   └── build.gradle.kts
├── gradle/
└── local.properties         # Auto-generated for Ubuntu
```

🐧 Ubuntu Tips

· Pastikan JAVA_HOME sudah set: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
· Android SDK path: /home/$USER/Android/Sdk
· Untuk build lebih cepat: ./gradlew assembleDebug --parallel

📱 Akses Dashboard

Setelah app berjalan:

· In-app: WebView internal
· Browser: http://localhost:9090/ui/
· Dari device lain: http://[IP-ADDRESS]:9090/ui/

