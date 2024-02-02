#!/bin/bash

# This script is intended to be used on Debian systems for building
# the project. It has been tested with Debian 8

USERNAME=$USER
SIGNING_NAME='k-9'
SDK_VERSION='r24.3.3'
SDK_DIR=$HOME/android-sdk

cd ..

PROJECT_HOME=$(pwd)

sudo apt-get install build-essential default-jdk \
     lib32stdc++6 lib32z1 lib32z1-dev

if [ ! -d $SDK_DIR ]; then
    mkdir -p $SDK_DIR
fi
cd $SDK_DIR

# download the SDK
if [ ! -f $SDK_DIR/android-sdk_$SDK_VERSION-linux.tgz ]; then
    wget https://dl.google.com/android/android-sdk_$SDK_VERSION-linux.tgz
    tar -xzvf android-sdk_$SDK_VERSION-linux.tgz
fi
SDK_DIR=$SDK_DIR/android-sdk-linux

echo 'Check that you have the SDK tools installed for Android 17, SDK 19.1'
if [ ! -f $SDK_DIR/tools/android ]; then
    echo "$SDK_DIR/tools/android not found"
    exit -1
fi
cd $SDK_DIR
chmod -R 0755 $SDK_DIR
chmod a+rx $SDK_DIR/tools

ANDROID_HOME=$SDK_DIR
echo "sdk.dir=$SDK_DIR" > $ANDROID_HOME/local.properties
PATH=${PATH}:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

android sdk
cd $PROJECT_HOME


if [ ! -f $SDK_DIR/tools/templates/gradle/wrapper/gradlew ]; then
    echo "$SDK_DIR/tools/templates/gradle/wrapper/gradlew not found"
    exit -2
fi
. $SDK_DIR/tools/templates/gradle/wrapper/gradlew build

#cd ~/develop/$PROJECT_NAME/build/outputs/apk
#keytool -genkey -v -keystore example.keystore -alias \
#    "$SIGNING_NAME" -keyalg RSA -keysize 4096
#jarsigner -verbose -keystore example.keystore \
#    k9mail-release-unsigned.apk "$SIGNING_NAME"

# cleaning up
cd $PROJECT_HOME/k9mail/build/outputs/apk
if [ ! -f k9mail-debug.apk ]; then
    echo 'k9mail-debug.apk was not found'
    exit -3
fi
echo 'Build script ended successfully'
echo -n 'apk is available at: '
echo "$PROJECT_HOME/k9mail/build/outputs/apk/k9mail-debug.apk"
exit 0
