#!/bin/bash
set -e

SCRIPT_PATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

# Enable and configure SystemUI demo mode
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e level 4 -e datatype lte
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false

pushd "${SCRIPT_PATH}/.."

# Build and install app
./gradlew :app:k9mail:installDebug

# Record screenshots
maestro test ui-flows/screenshots/user_manual_account_setup.yml
maestro test ui-flows/screenshots/user_manual_accounts.yml
maestro test ui-flows/screenshots/user_manual_reading.yml

# Post-process screenshots
user-manual/process_screenshots.sh

popd
