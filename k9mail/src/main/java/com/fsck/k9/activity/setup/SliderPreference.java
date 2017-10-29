/*
 * Copyright 2012 Jay Weisskopf
 *
 * Licensed under the MIT License (see LICENSE.txt)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Source: https://github.com/jayschwa/AndroidSliderPreference
 */

package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.support.annotation.ArrayRes;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import com.fsck.k9.*;


/**
 * @author Jay Weisskopf
 */
public class SliderPreference extends DialogPreference {
    private static final String STATE_KEY_SUPER = "super";
    private static final String STATE_KEY_SEEK_BAR_VALUE = "seek_bar_value";

    protected final static int SEEKBAR_RESOLUTION = 10000;

    protected float value;
    protected int seekBarValue;
    protected CharSequence[] summaries;

    /**
     * @param context
     * @param attrs
     */
    public SliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context, attrs);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public SliderPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup(context, attrs);
    }

    private void setup(Context context, AttributeSet attrs) {
        setDialogLayoutResource(R.layout.slider_preference_dialog);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SliderPreference);
        try {
            setSummary(a.getTextArray(R.styleable.SliderPreference_android_summary));
        } catch (Exception e) {
            // Do nothing
        }
        a.recycle();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getFloat(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedFloat(value) : (Float) defaultValue);
    }

    @Override
    public CharSequence getSummary() {
        if (summaries != null && summaries.length > 0) {
            int index = (int) (value * summaries.length);
            index = Math.min(index, summaries.length - 1);
            return summaries[index];
        } else {
            return super.getSummary();
        }
    }

    public void setSummary(CharSequence[] summaries) {
        this.summaries = summaries;
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
        summaries = null;
    }

    @Override
    public void setSummary(@ArrayRes int summaryResId) {
        try {
            setSummary(getContext().getResources().getStringArray(summaryResId));
        } catch (Exception e) {
            super.setSummary(summaryResId);
        }
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        value = Math.max(0, Math.min(value, 1)); // clamp to [0, 1]
        if (shouldPersist()) {
            persistFloat(value);
        }
        if (value != this.value) {
            this.value = value;
            notifyChanged();
        }
    }

    @Override
    protected View onCreateDialogView() {
        seekBarValue = (int) (value * SEEKBAR_RESOLUTION);
        View view = super.onCreateDialogView();
        SeekBar seekbar = (SeekBar) view.findViewById(R.id.slider_preference_seekbar);
        seekbar.setMax(SEEKBAR_RESOLUTION);
        seekbar.setProgress(seekBarValue);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    SliderPreference.this.seekBarValue = progress;
                    callChangeListener((float) SliderPreference.this.seekBarValue / SEEKBAR_RESOLUTION);
                }
            }
        });
        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        final float newValue = (float) seekBarValue / SEEKBAR_RESOLUTION;
        if (positiveResult && callChangeListener(newValue)) {
            setValue(newValue);
        } else {
            callChangeListener(value);
        }
        super.onDialogClosed(positiveResult);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        Bundle state = new Bundle();
        state.putParcelable(STATE_KEY_SUPER, superState);
        state.putInt(STATE_KEY_SEEK_BAR_VALUE, seekBarValue);

        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        super.onRestoreInstanceState(bundle.getParcelable(STATE_KEY_SUPER));
        seekBarValue = bundle.getInt(STATE_KEY_SEEK_BAR_VALUE);

        callChangeListener((float) seekBarValue / SEEKBAR_RESOLUTION);
    }
}
