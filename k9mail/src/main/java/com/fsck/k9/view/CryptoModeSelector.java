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

    public static final int CROSSFADE_THRESH_3_LOW = 125;
    public static final int CROSSFADE_THRESH_2_LOW = 50;
    public static final int CROSSFADE_THRESH_2_HIGH = 150;
    public static final float CROSSFADE_DIVISOR_2 = 50.0f;
    public static final float CROSSFADE_DIVISOR_3 = 75.0f;
    private SeekBar seekbar;
    private ImageView modeIcon2;
    private ImageView modeIcon3;

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
        inflate(getContext(), R.layout.crypto_settings_slider, this);
        seekbar = (SeekBar) findViewById(R.id.seek_bar);
        modeIcon2 = (ImageView) findViewById(R.id.icon_2);
        modeIcon3 = (ImageView) findViewById(R.id.icon_3);

        seekbar.setOnSeekBarChangeListener(this);
        onProgressChanged(seekbar, seekbar.getProgress(), false);

        // this one is set static for now, since it is static grey and not crossfaded
        ImageView modeIcon1 = (ImageView) findViewById(R.id.icon_1);
        modeIcon1.setColorFilter(getResources().getColor(R.color.openpgp_grey), PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int grey = getResources().getColor(R.color.openpgp_grey);

        float crossfadeValue2, crossfadeValue3;
        if (progress > CROSSFADE_THRESH_2_LOW && progress < CROSSFADE_THRESH_2_HIGH) {
            crossfadeValue2 = (progress -CROSSFADE_THRESH_2_LOW) / CROSSFADE_DIVISOR_2;
            if (crossfadeValue2 > 1.0f) {
                crossfadeValue2 = 2.0f -crossfadeValue2;
            }
        } else {
            crossfadeValue2 = 0.0f;
        }

        if (progress > CROSSFADE_THRESH_3_LOW) {
            crossfadeValue3 = (progress -CROSSFADE_THRESH_3_LOW) / CROSSFADE_DIVISOR_3;
        } else {
            crossfadeValue3 = 0.0f;
        }

        int crossfadedColor;

        crossfadedColor = crossfadeColor(grey, getResources().getColor(R.color.openpgp_red), crossfadeValue2);
        modeIcon2.setColorFilter(crossfadedColor, PorterDuff.Mode.SRC_ATOP);

        crossfadedColor = crossfadeColor(grey, getResources().getColor(R.color.openpgp_green), crossfadeValue3);
        modeIcon3.setColorFilter(crossfadedColor, PorterDuff.Mode.SRC_ATOP);
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
        } else {
            animateSnapTo(200);
            newCryptoStatus = 2;
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
        } else {
            seekbar.setProgress(200);
        }
    }

    public interface CryptoStatusSelectedListener {
        void onCryptoStatusSelected(int type);
    }

}
