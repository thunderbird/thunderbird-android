#!/bin/bash

# This script is intended to be used on Debian systems for building
# the project. It has been tested with Debian 8

USERNAME=$USER
SIGNING_NAME='k-9'

cd ..

PROJECT_HOME=$(pwd)

sudo apt-get install build-essential default-jdk android-tools-adb \
     lib32stdc++6 lib32z1 lib32z1-dev gradle

if [ ! -d ~/Android ]; then
    echo -n 'Download android studio from http://developer.android.com/sdk/'
    echo -n 'index.html and extract it to the the home folder, install, open'
    echo -n 'the Sdk Manager and install Android 17 and Sdk tools for 19.1, '
    echo 'then press any key...'
    read -n 1 -s
    cd ~/Android/Sdk/tools
    echo ''
    echo ''
    echo -n 'You need to have pretty much everything installed. '
    echo 'It will take some time.'
    ./android sdk
    cd $PROJECT_HOME
fi

echo "sdk.dir=/home/$USERNAME/Android/Sdk" > local.properties

# install Gradle (if is not installed yet) and then execute Gradle
./gradlew build

#cd ~/develop/$PROJECT_NAME/build/outputs/apk
#keytool -genkey -v -keystore example.keystore -alias \
#    "$SIGNING_NAME" -keyalg RSA -keysize 4096
#jarsigner -verbose -keystore example.keystore \
#    k9mail-release-unsigned.apk "$SIGNING_NAME"

# cleaning up
cd $PROJECT_HOME/k9mail/build/outputs/apk
if [ ! -f k9mail-release-unsigned.apk ]; then
    echo 'k9mail-release-unsigned.apk was not found'
    exit 648
fi
echo 'Build script ended successfully'
echo -n 'apk is available at: '
echo "$PROJECT_HOME/k9mail/build/outputs/apk/k9mail-release-unsigned.apk"
exit 0
