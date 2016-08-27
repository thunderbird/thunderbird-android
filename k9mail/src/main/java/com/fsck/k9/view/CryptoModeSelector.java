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


public class CryptoModeSelector extends FrameLayout implements OnSeekBarChangeListener {
    public static final int CROSSFADE_THRESH_2_LOW = 50;
    public static final int CROSSFADE_THRESH_2_HIGH = 150;
    public static final int CROSSFADE_THRESH_3_LOW = 150;
    public static final int CROSSFADE_THRESH_3_HIGH = 250;
    public static final int CROSSFADE_THRESH_4_LOW = 250;
    public static final float CROSSFADE_DIVISOR_2 = 50.0f;
    public static final float CROSSFADE_DIVISOR_3 = 50.0f;
    public static final float CROSSFADE_DIVISOR_4 = 50.0f;


    private SeekBar seekbar;
    private ImageView modeIcon2;
    private ImageView modeIcon3;
    private ImageView modeIcon4;

    private ObjectAnimator currentSeekbarAnim;

    private int currentCryptoStatus;

    private CryptoStatusSelectedListener cryptoStatusListener;
    private static final ArgbEvaluator ARGB_EVALUATOR = new ArgbEvaluator();


    public CryptoModeSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CryptoModeSelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        inflate(getContext(), R.layout.crypto_mode_selector, this);
        seekbar = (SeekBar) findViewById(R.id.seek_bar);
        modeIcon2 = (ImageView) findViewById(R.id.icon_2);
        modeIcon3 = (ImageView) findViewById(R.id.icon_3);
        modeIcon4 = (ImageView) findViewById(R.id.icon_4);

        seekbar.setOnSeekBarChangeListener(this);
        onProgressChanged(seekbar, seekbar.getProgress(), false);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int grey = ThemeUtils.getStyledColor(getContext(), R.attr.openpgp_grey);

        float crossfadeValue2, crossfadeValue3, crossfadeValue4;
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

        crossfadedColor = crossfadeColor(grey, ThemeUtils.getStyledColor(getContext(), R.attr.openpgp_blue), crossfadeValue2);
        modeIcon2.setColorFilter(crossfadedColor, PorterDuff.Mode.SRC_ATOP);

        crossfadedColor = crossfadeColor(grey, ThemeUtils.getStyledColor(getContext(), R.attr.openpgp_orange), crossfadeValue3);
        modeIcon3.setColorFilter(crossfadedColor, PorterDuff.Mode.SRC_ATOP);

        crossfadedColor = crossfadeColor(grey, ThemeUtils.getStyledColor(getContext(), R.attr.openpgp_green), crossfadeValue4);
        modeIcon4.setColorFilter(crossfadedColor, PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int newCryptoStatus;

        int progress = seekbar.getProgress();
        if (progress < 50) {
            animateSnapTo(0);
            newCryptoStatus = 0;
        } else if (progress < 150) {
            animateSnapTo(100);
            newCryptoStatus = 1;
        } else if (progress < 250) {
            animateSnapTo(200);
            newCryptoStatus = 2;
        } else {
            animateSnapTo(300);
            newCryptoStatus = 3;
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

    public void setCryptoStatus(int status) {
        currentCryptoStatus = status;
        if (status == 0) {
            seekbar.setProgress(0);
        } else if (status == 1) {
            seekbar.setProgress(100);
        } else if (status == 2) {
            seekbar.setProgress(200);
        } else {
            seekbar.setProgress(300);
        }
    }

    public interface CryptoStatusSelectedListener {
        void onCryptoStatusSelected(int type);
    }

}
