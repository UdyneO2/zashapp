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
rm -f release.keystore
rm -f keystore.properties
echo -e "${GREEN}✓ Keystore lama dihapus${NC}"

# Input detail
echo ""
echo -e "${YELLOW}Masukkan detail keystore (Enter untuk default):${NC}"
echo ""

STORE_PASS=${STORE_PASS:-android}
KEY_PASS=${KEY_PASS:-android}
KEY_ALIAS=${KEY_ALIAS:-udyneos}
NAME=${NAME:-Udyneos}
OU=${OU:-Development}
ORG=${ORG:-Udyneos}
CITY=${CITY:-Jakarta}
STATE=${STATE:-Jakarta}
COUNTRY=ID

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
