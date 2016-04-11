package com.fsck.k9.mail.internet;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;

import com.fsck.k9.R;

public enum SecureTransportState {
    UNKNOWN(R.drawable.status_lock_open, R.color.crypto_blue),
    SECURE(R.drawable.status_lock_closed, R.color.crypto_green),
    INSECURE(R.drawable.status_lock_open, R.color.crypto_red);

    private final int drawableId;
    private final int colorId;

    SecureTransportState(@DrawableRes int drawableId, @ColorRes int colorId) {
        this.drawableId = drawableId;
        this.colorId = colorId;
    }

    @DrawableRes
    public int getDrawableId() {
        return drawableId;
    }

    @ColorRes
    public int getColorId() {
        return colorId;
    }
}
