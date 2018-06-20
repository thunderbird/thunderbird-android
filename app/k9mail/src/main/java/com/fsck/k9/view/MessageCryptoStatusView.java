package com.fsck.k9.view;


import android.content.Context;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.fsck.k9.R;


public class MessageCryptoStatusView extends FrameLayout {

    private ImageView iconSingle;

    public MessageCryptoStatusView(Context context) {
        super(context);
    }

    public MessageCryptoStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageCryptoStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        iconSingle = (ImageView) findViewById(R.id.crypto_status_single);
    }

    public void setCryptoDisplayStatus(MessageCryptoDisplayStatus displayStatus) {
        @ColorInt int color = ThemeUtils.getStyledColor(getContext(), displayStatus.colorAttr);

        iconSingle.setImageResource(displayStatus.statusIconRes);
        iconSingle.setColorFilter(color);
    }
}
