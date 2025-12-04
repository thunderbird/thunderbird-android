package com.tokenautocomplete;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;

/**
 * Invisible MetricAffectingSpan that will trigger a redraw when it is being added to or removed from an Editable.
 *
 * @see TokenCompleteTextView#redrawTokens()
 */
class DummySpan extends MetricAffectingSpan {
    static final DummySpan INSTANCE = new DummySpan();

    private DummySpan() {}

    @Override
    public void updateMeasureState(@NonNull TextPaint textPaint) {}

    @Override
    public void updateDrawState(TextPaint tp) {}
}
