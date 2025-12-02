#!/bin/bash

# ==============================================================================
# Android App Cycler
# Opens and closes a specified Android package repeatedly via ADB.
# ==============================================================================

# 1. Validate Arguments
if [ -z "$1" ]; then
    echo "Error: Package name required."
    echo "Usage: ./app_cycler.sh <package_name> [iterations] [load_wait_seconds]"
    echo "Example: ./app_cycler.sh net.thunderbird.android 10 5"
    exit 1
fi

PACKAGE_NAME=$1
ITERATIONS=${2:-5}  # Default to 5 iterations if not specified
LOAD_WAIT=${3:-5}   # Default to 5 seconds wait if not specified
COOLDOWN=2          # Seconds to wait after closing before restarting

echo "Target Package: $PACKAGE_NAME"
echo "Total Cycles:   $ITERATIONS"
echo "Load Wait:      ${LOAD_WAIT}s"
echo "----------------------------------------"

for ((i=1; i<=ITERATIONS; i++)); do
    echo "[Cycle $i/$ITERATIONS] Starting..."

    # Clear logcat buffer to ensure we only catch crashes from this cycle
    adb shell logcat -c

    # 2. Launch the App
    # We use 'monkey' because it automatically finds the launchable intent
    adb shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1

    # 3. Detect "Loaded" State
    echo "  - Waiting for app focus..."
    IS_FOCUSED=false
    HAS_CRASHED=false
    ATTEMPT=0
    MAX_ATTEMPTS=20

    while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
        # Crash Check 1: Scan logcat for Fatal Exceptions linked to the package
        CRASH_LOG=$(adb shell "logcat -d | grep -E 'FATAL EXCEPTION' | grep '$PACKAGE_NAME'")
        if [ -n "$CRASH_LOG" ]; then
            echo "  ! CRASH DETECTED: System reported a fatal exception before UI appeared."
            echo "    Log snippet: $(echo "$CRASH_LOG" | head -n 1)"
            HAS_CRASHED=true
            break
        fi

        # specific grep handles different android versions (mCurrentFocus vs mFocusedApp)
        CURRENT_FOCUS=$(adb shell dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp')

        if [[ "$CURRENT_FOCUS" == *"$PACKAGE_NAME"* ]]; then
            IS_FOCUSED=true
            break
        fi
        sleep 1
        ((ATTEMPT++))
    done

    if [ "$IS_FOCUSED" = true ]; then
        echo "  - App focused. Monitoring stability for ${LOAD_WAIT}s..."

        # Loop for load wait to catch crashes during load (Crash Check 2)
        for ((w=0; w<LOAD_WAIT; w++)); do
            sleep 1
            # Check logcat again for crashes that happen after focus (during load)
            CRASH_LOG=$(adb shell "logcat -d | grep -E 'FATAL EXCEPTION' | grep '$PACKAGE_NAME'")
            if [ -n "$CRASH_LOG" ]; then
                echo "  ! CRASH DETECTED: System reported a fatal exception during load time."
                echo "    Log snippet: $(echo "$CRASH_LOG" | head -n 1)"
                HAS_CRASHED=true
                break
            fi
        done
    elif [ "$HAS_CRASHED" = false ]; then
        echo "  - WARNING: App took too long to focus (no crash log found). Proceeding anyway."
    fi

    # 4. Force Close the App
    echo "  - Closing app..."
    adb shell am force-stop "$PACKAGE_NAME"

    # 5. Cooldown period
    if [ $i -lt $ITERATIONS ]; then
        sleep $COOLDOWN
    fi
    echo ""
done

echo "Done. Completed $ITERATIONS cycles."
