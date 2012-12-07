package com.fsck.k9.helper;

public final class StringUtils {

    public static boolean isNullOrEmpty(String string){
        return string == null || string.length() == 0;
    }

    public static boolean containsAny(String haystack, String[] needles) {
        if (haystack == null) {
            return false;
        }

        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }

        return false;
    }
}
