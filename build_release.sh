#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}================================${NC}"
echo -e "${GREEN}MEMBUAT SIGNED RELEASE APK${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

# Cek keystore
if [ ! -f "release.keystore" ]; then
    echo -e "${RED}Keystore tidak ditemukan!${NC}"
    echo "Generate dulu dengan: ./generate_keystore.sh"
    exit 1
fi

# Cek keystore.properties
if [ ! -f "keystore.properties" ]; then
    echo -e "${RED}keystore.properties tidak ditemukan!${NC}"
    echo "Copy dari example: cp keystore.properties.example keystore.properties"
    exit 1
fi

# Bersihkan
echo -e "${YELLOW}[1/3] Membersihkan project...${NC}"
./gradlew clean

# Build release
echo -e "${YELLOW}[2/3] Membangun release APK...${NC}"
./gradlew assembleRelease

if [ $? -ne 0 ]; then
    echo -e "${RED}Build gagal!${NC}"
    exit 1
fi

# Cari APK hasil build
RELEASE_APK="app/build/outputs/apk/release/app-release.apk"

if [ -f "$RELEASE_APK" ]; then
    echo -e "${YELLOW}[3/3] Menyalin APK...${NC}"
    
    # Buat nama file dengan timestamp
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    FINAL_APK="Zashboard-v1.0.0-release-${TIMESTAMP}.apk"
    
    cp "$RELEASE_APK" "$FINAL_APK"
    
    # Info APK
    SIZE=$(ls -lh "$FINAL_APK" | awk '{print $5}')
    
    echo ""
    echo -e "${GREEN}✅ SIGNED APK BERHASIL!${NC}"
    echo -e "${BLUE}File: ${NC}$FINAL_APK"
    echo -e "${BLUE}Ukuran: ${NC}$SIZE"
    echo -e "${BLUE}Lokasi asli: ${NC}$RELEASE_APK"
    echo ""
    echo -e "${YELLOW}Install dengan:${NC}"
    echo "adb install -r $FINAL_APK"
else
    echo -e "${RED}APK tidak ditemukan di $RELEASE_APK${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}SELESAI!${NC}"
echo -e "${GREEN}================================${NC}"
