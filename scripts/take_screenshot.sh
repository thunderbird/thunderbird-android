#!/bin/bash

# This script takes a screenshot of the connected device using adb.
# The screenshot is saved in the adb-screenshots directory with the current date and time as the file name.
# See https://android.googlesource.com/platform/frameworks/base/+/master/packages/SystemUI/docs/demo_mode.md

TARGET_DIR="./adb-screenshots/"
FILE_NAME="$(date +"%Y-%m-%d_%H-%M-%S").png"

start_demo_mode() {
    adb shell settings put global sysui_demo_allowed 1
    adb shell am broadcast -a com.android.systemui.demo -e command enter
    adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0058
    adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
}

end_demo_mode() {
    adb shell am broadcast -a com.android.systemui.demo -e command exit
    adb shell settings put global sysui_demo_allowed 0
}

mkdir -p $TARGET_DIR

echo "waiting for device..."
adb wait-for-device

start_demo_mode

mkdir -p $TARGET_DIR
adb exec-out screencap -p > $TARGET_DIR$FILE_NAME

end_demo_mode

echo ""
echo "File: $TARGET_DIR$FILE_NAME"
