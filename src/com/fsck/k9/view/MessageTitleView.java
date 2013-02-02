package com.fsck.k9.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * This {@link TextView} is used in the custom view of the {@link com.fsck.k9.activity.MessageList}
 * action bar.
 * It will hide the subject line in {@link MessageHeader} if the subject fits completely into the
 * action bar's title view.
 */
public class MessageTitleView extends TextView {
    private MessageHeader mHeader;

    public MessageTitleView(Context context) {
        this(context, null);
    }

    public MessageTitleView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public MessageTitleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Check to see if we need to hide the subject line in {@link MessageHeader} or not.
     */
    @Override
    public void onDraw(Canvas canvas) {
        if (mHeader != null && getLayout() != null && getLayout().getEllipsisCount(1) == 0) {
            mHeader.hideSubjectLine();
        }
        super.onDraw(canvas);
    }

    public void setMessageHeader(final MessageHeader header) {
        mHeader = header;
    }
}
