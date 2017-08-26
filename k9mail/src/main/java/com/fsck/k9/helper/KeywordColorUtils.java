package com.fsck.k9.helper;

import android.support.v4.graphics.ColorUtils;

import com.fsck.k9.mail.Keyword;

public class KeywordColorUtils implements Keyword.ColorUtils {
    public int blendARGB(int color1, int color2, float ratio) {
        return android.support.v4.graphics.ColorUtils.blendARGB(
            color1, color2, ratio);
    }
}
