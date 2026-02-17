#!/bin/bash

# Script untuk generate placeholder icons di Ubuntu
# Memerlukan imagemagick: sudo apt install imagemagick

set -e

echo "🎨 Generating placeholder icons..."
echo "==================================="

if ! command -v convert &> /dev/null; then
    echo "❌ ImageMagick tidak ditemukan. Install dengan:"
    echo "   sudo apt install imagemagick"
    exit 1
fi

ICON_DIR="app/src/main/res"
mkdir -p "$ICON_DIR/mipmap-hdpi"
mkdir -p "$ICON_DIR/mipmap-mdpi"
mkdir -p "$ICON_DIR/mipmap-xhdpi"
mkdir -p "$ICON_DIR/mipmap-xxhdpi"
mkdir -p "$ICON_DIR/mipmap-xxxhdpi"

# Create a simple blue square with "Z" text as icon
create_icon() {
    SIZE=$1
    OUTPUT="$ICON_DIR/mipmap-$2/ic_launcher.png"
    ROUND_OUTPUT="$ICON_DIR/mipmap-$2/ic_launcher_round.png"
    
    # Create square icon
    convert -size ${SIZE}x${SIZE} xc:#2196F3 \
        -fill white -gravity center -pointsize $((SIZE/2)) -font Ubuntu -annotate 0 "Z" \
        "$OUTPUT"
    
    # Create round icon
    convert -size ${SIZE}x${SIZE} xc:#2196F3 \
        -fill white -gravity center -pointsize $((SIZE/2)) -font Ubuntu -annotate 0 "Z" \
        \( +clone -alpha extract -draw "circle $((SIZE/2)),$((SIZE/2)) $((SIZE/2)),1" -alpha copy \) \
        "$ROUND_OUTPUT"
    
    echo "   ✅ Created $2 ($SIZE x $SIZE)"
}

# Generate icons for different densities
create_icon 48 "hdpi"
create_icon 36 "mdpi"  # MDPI biasanya 36x36 untuk launcher
create_icon 72 "xhdpi"
create_icon 96 "xxhdpi"
create_icon 144 "xxxhdpi"

echo ""
echo "✅ Icons generated successfully!"
