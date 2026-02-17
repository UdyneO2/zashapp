#!/bin/bash

echo "🔍 Testing setup..."
echo "==================="

echo "1. Java version:"
java -version
echo ""

echo "2. JAVA_HOME:"
echo $JAVA_HOME
echo ""

echo "3. Android SDK:"
echo $ANDROID_HOME
ls -la $ANDROID_HOME/build-tools/
echo ""

echo "4. Gradle wrapper:"
./gradlew --version | head -n 5
echo ""

echo "5. Testing gradle tasks:"
./gradlew tasks --no-daemon
