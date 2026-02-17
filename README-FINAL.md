# Udyneos Zashboard - Complete Documentation

## 📱 Aplikasi Android untuk Zashboard Dashboard

### Fitur Lengkap
- ✅ Server HTTP lokal di port 9090
- ✅ WebView terintegrasi
- ✅ Foreground service dengan notifikasi
- ✅ PWA Support
- ✅ URL Parameters untuk konfigurasi
- ✅ Dark mode support
- ✅ Material Design 3
- ✅ Optimasi untuk Ubuntu build

## 🚀 Quick Start di Ubuntu

### 1. Install Dependencies
```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Java, Git, dan tools
sudo apt install -y openjdk-17-jdk git curl wget unzip

# Install Android SDK (jika belum)
mkdir -p ~/Android/Sdk
cd ~/Android/Sdk
wget https://dl.google.com/android/repository/commandlinetools-linux-latest.zip
unzip commandlinetools-linux-latest.zip
rm commandlinetools-linux-latest.zip

# Set environment variables
echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc

# Install platform tools
sdkmanager "platform-tools" "platforms;android-34"
