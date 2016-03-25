package com.fsck.k9.mail.internet;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;

import com.fsck.k9.R;

/**
 * Created by philip on 25/03/2016.
 */
public enum SecureTransportState {
    UNKNOWN(R.drawable.status_lock_open, R.color.transportSecurity_blue),
    SECURE(R.drawable.status_lock_closed, R.color.transportSecurity_green),
    INSECURE(R.drawable.status_lock_open, R.color.transportSecurity_red);

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
