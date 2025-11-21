package com.tokenautocomplete;

import android.text.Layout;
import android.text.TextPaint;
import android.text.style.CharacterStyle;

import java.util.Locale;

/**
 * Span that displays +[x]
 *
 * Created on 2/3/15.
 * @author mgod
 */

class CountSpan extends CharacterStyle {
    private String countText;

    CountSpan() {
        super();
        countText = "";
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        //Do nothing, we are using this span as a location marker
    }

    void setCount(int c) {
        if (c > 0) {
            countText = String.format(Locale.getDefault(), " +%d", c);
        } else {
            countText = "";
        }
    }

    String getCountText() {
        return countText;
    }

    float getCountTextWidthForPaint(TextPaint paint) {
        return Layout.getDesiredWidth(countText, 0, countText.length(), paint);
    }
}
