package com.fsck.k9.helper;

import android.os.Build;

/**
 * Helperclass to provide easy to use support for different codepaths for
 * different android versions
 *
 * @author Bernhard Redl
 *
 */
public class VersionHelper {

    /**
     * check if the required level is met. (>=)
     * @param requiredLevel
     * @return true if the Device api level is >= level
     * @see http://developer.android.com/reference/android/os/Build.VERSION_CODES.html
     */
    public static boolean ApiLevel(int requiredLevel) {
        return Build.VERSION.SDK_INT >= requiredLevel;
    }

    /**
     * check if the device version if BEFORE the given level.
     * Means < in math.
     * @param beforeLevel device < beforLevel
     * @return true if sdk version < beforeLevel
     */
    public static boolean ApiLevelPre(int beforeLevel) {
        return Build.VERSION.SDK_INT < beforeLevel;
    }

    /**
     * check if the device version if AFTER the given level. (>)
     * Means > in math.
     * @param aboveLevel device > aboveLevel
     * @return true if sdk version > aboveLevel
     */
    public static boolean ApiLevelAbove(int aboveLevel) {
        return Build.VERSION.SDK_INT > aboveLevel;
    }
}
