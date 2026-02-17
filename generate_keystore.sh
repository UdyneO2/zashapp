#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}================================${NC}"
echo -e "${GREEN}GENERATE KEYSTORE${NC}"
echo -e "${BLUE}================================${NC}"

# Hapus keystore lama jika ada
if [ -f "release.keystore" ]; then
    echo -e "${YELLOW}Keystore sudah ada.${NC}"
    read -p "Hapus dan buat baru? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        rm -f release.keystore
        rm -f keystore.properties
        echo -e "${GREEN}✓ Keystore lama dihapus${NC}"
    else
        exit 0
    fi
fi

# Input detail
echo ""
echo -e "${YELLOW}Masukkan detail keystore (Enter untuk default):${NC}"
echo ""

read -p "Keystore password [android]: " STORE_PASS
STORE_PASS=${STORE_PASS:-android}

read -p "Key password [android]: " KEY_PASS
KEY_PASS=${KEY_PASS:-android}

read -p "Key alias [udyneos]: " KEY_ALIAS
KEY_ALIAS=${KEY_ALIAS:-udyneos}

read -p "Your name [Udyneos]: " NAME
NAME=${NAME:-Udyneos}

read -p "Organization unit [Development]: " OU
OU=${OU:-Development}

read -p "Organization [Udyneos]: " ORG
ORG=${ORG:-Udyneos}

read -p "City/Locality [Jakarta]: " CITY
CITY=${CITY:-Jakarta}

read -p "State/Province [Jakarta]: " STATE
STATE=${STATE:-Jakarta}

read -p "Country Code (2 letters) [ID]: " COUNTRY
COUNTRY=${COUNTRY:-ID}

echo ""
echo -e "${YELLOW}Membuat keystore...${NC}"

# Generate keystore
keytool -genkey -v \
    -keystore release.keystore \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=$NAME, OU=$OU, O=$ORG, L=$CITY, ST=$STATE, C=$COUNTRY" \
    -storepass "$STORE_PASS" \
    -keypass "$KEY_PASS"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Keystore berhasil dibuat${NC}"
    
    # Buat keystore.properties - FIX: Variable expansion dengan double quotes
    cat > keystore.properties << EOF
storePassword=$STORE_PASS
keyPassword=$KEY_PASS
keyAlias=$KEY_ALIAS
storeFile=release.keystore
EOF
    
    echo -e "${GREEN}✓ keystore.properties berhasil dibuat${NC}"
    
    echo ""
    echo -e "${BLUE}================================${NC}"
    echo -e "${GREEN}KEYSTORE INFO${NC}"
    echo -e "${BLUE}================================${NC}"
    echo "Keystore file: release.keystore"
    echo "Key alias: $KEY_ALIAS"
    echo "Store password: [protected]"
    echo "Key password: [protected]"
    echo ""
    echo -e "${YELLOW}Test signing:${NC}"
    echo "keytool -list -v -keystore release.keystore -alias $KEY_ALIAS -storepass $STORE_PASS"
else
    echo -e "${RED}❌ Gagal membuat keystore${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✅ SELESAI!${NC}"


chmod +x generate_keystore.sh

# Buat keystore.properties.example
cat > keystore.properties.example << 'EOF'
storePassword=android
keyPassword=android
keyAlias=udyneos
storeFile=release.keystore
EOF
