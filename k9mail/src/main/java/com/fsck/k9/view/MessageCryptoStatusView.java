package com.fsck.k9.view;


import android.content.Context;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.fsck.k9.R;


public class MessageCryptoStatusView extends FrameLayout {

    private ImageView iconSingle;
    private ImageView iconCombinedFirst;
    private ImageView iconCombinedSecond;
    private ImageView iconDotsBackground;

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
        iconCombinedFirst = (ImageView) findViewById(R.id.crypto_status_combined_1);
        iconCombinedSecond = (ImageView) findViewById(R.id.crypto_status_combined_2);
        iconDotsBackground = (ImageView) findViewById(R.id.crypto_status_dots_bg);
    }

    public void setCryptoDisplayStatus(MessageCryptoDisplayStatus displayStatus) {
        @ColorInt int color = ThemeUtils.getStyledColor(getContext(), displayStatus.colorAttr);

        if (displayStatus.statusDotsRes != null) {
            iconCombinedFirst.setVisibility(View.VISIBLE);
            iconCombinedSecond.setVisibility(View.VISIBLE);
            iconDotsBackground.setVisibility(View.VISIBLE);
            iconSingle.setVisibility(View.GONE);

            iconCombinedFirst.setImageResource(displayStatus.statusIconRes);
            iconCombinedFirst.setColorFilter(color);
            iconCombinedSecond.setImageResource(displayStatus.statusDotsRes);
            iconCombinedSecond.setColorFilter(color);
        } else {
            iconCombinedFirst.setVisibility(View.GONE);
            iconCombinedSecond.setVisibility(View.GONE);
            iconDotsBackground.setVisibility(View.GONE);
            iconSingle.setVisibility(View.VISIBLE);

            iconSingle.setImageResource(displayStatus.statusIconRes);
            iconSingle.setColorFilter(color);
        }
    }
}
