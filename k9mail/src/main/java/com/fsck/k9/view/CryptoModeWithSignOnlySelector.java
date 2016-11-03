package com.fsck.k9.view;


import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.fsck.k9.R;


public class CryptoModeWithSignOnlySelector extends FrameLayout implements CryptoModeSelector, OnSeekBarChangeListener {
    public static final int CROSSFADE_THRESH_1_LOW = -50;
    public static final int CROSSFADE_THRESH_1_HIGH = 50;
    public static final int CROSSFADE_THRESH_2_LOW = 50;
    public static final int CROSSFADE_THRESH_2_HIGH = 150;
    public static final int CROSSFADE_THRESH_3_LOW = 150;
    public static final int CROSSFADE_THRESH_3_HIGH = 250;
    public static final int CROSSFADE_THRESH_4_LOW = 250;
    public static final float CROSSFADE_DIVISOR_1 = 50.0f;
    public static final float CROSSFADE_DIVISOR_2 = 50.0f;
    public static final float CROSSFADE_DIVISOR_3 = 50.0f;
    public static final float CROSSFADE_DIVISOR_4 = 50.0f;


    private SeekBar seekbar;
    private ImageView modeIconDisabled;
    private ImageView modeIconSignOnly;
    private ImageView modeIconOpportunistic;
    private ImageView modeIconPrivate;

    private ObjectAnimator currentSeekbarAnim;

    private CryptoModeSelectorState currentCryptoStatus;

    private CryptoStatusSelectedListener cryptoStatusListener;
    private static final ArgbEvaluator ARGB_EVALUATOR = new ArgbEvaluator();


    public CryptoModeWithSignOnlySelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CryptoModeWithSignOnlySelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        inflate(getContext(), R.layout.crypto_mode_with_sign_only_selector, this);
        seekbar = (SeekBar) findViewById(R.id.seek_bar);
        modeIconDisabled = (ImageView) findViewById(R.id.icon_disabled);
        modeIconSignOnly = (ImageView) findViewById(R.id.icon_sign_only);
        modeIconOpportunistic = (ImageView) findViewById(R.id.icon_opportunistic);
        modeIconPrivate = (ImageView) findViewById(R.id.icon_private);

        seekbar.setOnSeekBarChangeListener(this);
        onProgressChanged(seekbar, seekbar.getProgress(), false);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int grey = ThemeUtils.getStyledColor(getContext(), R.attr.openpgp_grey);

        float crossfadeValue1, crossfadeValue2, crossfadeValue3, crossfadeValue4;

        if (progress < CROSSFADE_THRESH_1_HIGH) {
            crossfadeValue1 = (progress -CROSSFADE_THRESH_1_LOW) / CROSSFADE_DIVISOR_1;
            if (crossfadeValue1 > 1.0f) {
                crossfadeValue1 = 2.0f -crossfadeValue1;
            }
        } else {
            crossfadeValue1 = 0.0f;
        }


        if (progress > CROSSFADE_THRESH_2_LOW && progress < CROSSFADE_THRESH_2_HIGH) {
            crossfadeValue2 = (progress -CROSSFADE_THRESH_2_LOW) / CROSSFADE_DIVISOR_2;
            if (crossfadeValue2 > 1.0f) {
                crossfadeValue2 = 2.0f -crossfadeValue2;
            }
        } else {
            crossfadeValue2 = 0.0f;
        }

        if (progress > CROSSFADE_THRESH_3_LOW && progress < CROSSFADE_THRESH_3_HIGH) {
            crossfadeValue3 = (progress -CROSSFADE_THRESH_3_LOW) / CROSSFADE_DIVISOR_3;
            if (crossfadeValue3 > 1.0f) {
                crossfadeValue3 = 2.0f -crossfadeValue3;
            }
        } else {
            crossfadeValue3 = 0.0f;
        }

        if (progress > CROSSFADE_THRESH_4_LOW) {
            crossfadeValue4 = (progress -CROSSFADE_THRESH_4_LOW) / CROSSFADE_DIVISOR_4;
        } else {
            crossfadeValue4 = 0.0f;
        }

        int crossfadedColor;

        crossfadedColor = crossfadeColor(grey, ThemeUtils.getStyledColor(getContext(), R.attr.openpgp_dark_grey), crossfadeValue1);
        modeIconDisabled.setColorFilter(crossfadedColor, PorterDuff.Mode.SRC_ATOP);

        crossfadedColor = crossfadeColor(grey, ThemeUtils.getStyledColor(getContext(), R.attr.openpgp_blue), crossfadeValue2);
        modeIconSignOnly.setColorFilter(crossfadedColor, PorterDuff.Mode.SRC_ATOP);

        crossfadedColor = crossfadeColor(grey, ThemeUtils.getStyledColor(getContext(), R.attr.openpgp_orange), crossfadeValue3);
        modeIconOpportunistic.setColorFilter(crossfadedColor, PorterDuff.Mode.SRC_ATOP);

        crossfadedColor = crossfadeColor(grey, ThemeUtils.getStyledColor(getContext(), R.attr.openpgp_green), crossfadeValue4);
        modeIconPrivate.setColorFilter(crossfadedColor, PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        CryptoModeSelectorState newCryptoStatus;

        int progress = seekbar.getProgress();
        if (progress < 50) {
            animateSnapTo(0);
            newCryptoStatus = CryptoModeSelectorState.DISABLED;
        } else if (progress < 150) {
            animateSnapTo(100);
            newCryptoStatus = CryptoModeSelectorState.SIGN_ONLY;
        } else if (progress < 250) {
            animateSnapTo(200);
            newCryptoStatus = CryptoModeSelectorState.OPPORTUNISTIC;
        } else {
            animateSnapTo(300);
            newCryptoStatus = CryptoModeSelectorState.PRIVATE;
        }

        if (currentCryptoStatus != newCryptoStatus) {
            currentCryptoStatus = newCryptoStatus;
            cryptoStatusListener.onCryptoStatusSelected(newCryptoStatus);
        }
    }

    private void animateSnapTo(int value) {
        if (currentSeekbarAnim != null) {
            currentSeekbarAnim.cancel();
        }
        currentSeekbarAnim = ObjectAnimator.ofInt(seekbar, "progress", seekbar.getProgress(), value);
        currentSeekbarAnim.setDuration(150);
        currentSeekbarAnim.start();
    }

    public static int crossfadeColor(int color1, int color2, float factor) {
        return (Integer) ARGB_EVALUATOR.evaluate(factor, color1, color2);
    }

    public void setCryptoStatusListener(CryptoStatusSelectedListener cryptoStatusListener) {
        this.cryptoStatusListener = cryptoStatusListener;
    }

    public void setCryptoStatus(CryptoModeSelectorState status) {
        currentCryptoStatus = status;
        switch (status) {
            case DISABLED:
                seekbar.setProgress(0);
                break;
            case SIGN_ONLY:
                seekbar.setProgress(100);
                break;
            case OPPORTUNISTIC:
                seekbar.setProgress(200);
                break;
            case PRIVATE:
                seekbar.setProgress(300);
                break;
        }
    }
}
