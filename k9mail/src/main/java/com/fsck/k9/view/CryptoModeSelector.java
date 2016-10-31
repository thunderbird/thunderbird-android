package com.fsck.k9.view;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;


public abstract class CryptoModeSelector extends FrameLayout {
    public CryptoModeSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CryptoModeSelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public abstract void setCryptoStatusListener(CryptoStatusSelectedListener cryptoStatusListener);

    public abstract void setCryptoStatus(CryptoModeSelectorState status);

    public interface CryptoStatusSelectedListener {
        void onCryptoStatusSelected(CryptoModeSelectorState type);
    }

    public enum CryptoModeSelectorState {
        DISABLED, SIGN_ONLY, OPPORTUNISTIC, PRIVATE
    }
}
