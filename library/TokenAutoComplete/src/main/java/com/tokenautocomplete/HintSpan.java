package com.tokenautocomplete;

import android.content.res.ColorStateList;
import android.text.style.TextAppearanceSpan;

/**
 * Subclass of TextAppearanceSpan just to work with how Spans get detected
 *
 * Created on 2/3/15.
 * @author mgod
 */
class HintSpan extends TextAppearanceSpan {
    HintSpan(String family, int style, int size, ColorStateList color, ColorStateList linkColor) {
        super(family, style, size, color, linkColor);
    }
}
