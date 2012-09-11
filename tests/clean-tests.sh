#!/bin/sh

# clean and run all the tests on an emulator -- only one emulator should be running (a device can be attached).
# will start and stop an emulator if no emulator is running.
# name of emulator is given as an argument (no spaces in the name!), or "api7" if not given.
# starting the emulator requires daemonize which requires a Unix-like system: http://software.clapper.org/daemonize/

# clean
ant all clean || exit 99

# see if emulator is running, and uninstall package if so
EMULATOR_ALREADY_RUNNING=false
if adb devices | grep emulator | grep device$; then
    ant -Dadb.device.arg=-e uninstall || exit 98
    EMULATOR_ALREADY_RUNNING=true
fi

# build project and test project
time ant emma debug artifacts || exit 1

# start emulator if not running, and uninstall package
if [ $EMULATOR_ALREADY_RUNNING == false ] ; then
    if [ -z $1 ]; then
        AVD_NAME=api7
    else
        AVD_NAME=$1
    fi

    echo starting emulator ${AVD_NAME}
    daemonize -o /tmp/${AVD_NAME}.stdout -e /tmp/${AVD_NAME}.stderr -p /tmp/${AVD_NAME}.pid -l /tmp/${AVD_NAME}.lock \
            $ANDROID_HOME/tools/emulator-arm -avd ${AVD_NAME} -no-audio -no-window -no-snapshot-save || exit 97
    ps ux | grep -f /tmp/${AVD_NAME}.pid | grep emulator || exit 96
    adb kill-server
    time adb start-server
    adb devices
    #sleep 7
    adb devices | grep emulator || exit 95
    echo adb -e wait-for-device
    time adb -e wait-for-device
    adb devices | grep device$ || exit 94
    ant -Dadb.device.arg=-e uninstall || exit 98
fi

# install project and test project, run tests
time ant -Dadb.device.arg=-e emma installd test || exit 2

# lint, javadoc, monkey
cd ..
time ant lint-xml || exit 3
time ant javadoc || exit 4
time ant -Dmonkey.count=200 -Dmonkey.seed=0 monkey || exit 5

# kill emulator if this script started it
if [ $EMULATOR_ALREADY_RUNNING == false ] ; then
    adb emu kill || exit 93
    sleep 1
    ! ps ux | grep -f /tmp/${AVD_NAME}.pid | grep emulator || exit 92
    rm -f /tmp/${AVD_NAME}.stdout /tmp/${AVD_NAME}.stderr /tmp/${AVD_NAME}.pid /tmp/${AVD_NAME}.lock
fi
