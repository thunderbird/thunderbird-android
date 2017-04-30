package com.fsck.k9.view;


import android.content.Context;
import android.content.res.Resources.Theme;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.UiThread;
import android.util.TypedValue;


public class ThemeUtils {
    private static final TypedValue TYPED_VALUE = new TypedValue();

    @ColorInt
    public static int getStyledColor(Context context, @AttrRes int attr) {
        return getStyledColor(context.getTheme(), attr);
    }

    @ColorInt
    @UiThread
    public static int getStyledColor(Theme theme, @AttrRes int attr) {
        theme.resolveAttribute(attr, TYPED_VALUE, true);
        return TYPED_VALUE.data;
    }
}
