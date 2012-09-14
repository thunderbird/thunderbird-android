package com.fsck.k9.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;
import com.fsck.k9.K9;

/**
 * This {@link TextView} is used in the title of the {@link com.fsck.k9.activity.MessageView} ActionBar.
 * It'll un-hide the subject line {@link MessageHeader} if it doesn't fit in the ActionBar's title area.
 */
public class MessageTitleView extends TextView {
    private static final String LOG_PREFIX = "MessageTitleView: ";
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
     * Check to see if we need to unhide the subject line in the MessageHeader or not.
     * @param canvas Canvas to draw on.
     */
    @Override
    public void onDraw(Canvas canvas) {
        if(mHeader != null && getLayout() != null) {
            if(getLayout().getEllipsisCount(1) > 0) {
                if(K9.DEBUG) {
                    Log.d(K9.LOG_TAG, LOG_PREFIX +
                        "Subject was truncated; enabling the subject line in the message header.");
                }
                mHeader.showSubjectLine();
            } else {
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, LOG_PREFIX + "Subject was fully shown in ActionBar.");
                }
            }
        }
        super.onDraw(canvas);
    }

    public void setMessageHeader(final MessageHeader header) {
        this.mHeader = header;
    }
}
