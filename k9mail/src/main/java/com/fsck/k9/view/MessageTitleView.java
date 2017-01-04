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
    private static final int MAX_LINES = 2;
    private static final String ELLIPSIS = "\u2026";

    private MessageHeader mHeader;
    private boolean mNeedEllipsizeCheck = true;

    public MessageTitleView(Context context) {
        this(context, null);
    }

    public MessageTitleView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public MessageTitleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start,
            int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        mNeedEllipsizeCheck = true;
    }
    /**
     * Check to see if we need to hide the subject line in {@link MessageHeader} or not.
     */
    @Override
    public void onDraw(Canvas canvas) {
        /*
         * Android does not support ellipsize in combination with maxlines
         * for TextViews. To work around that, check for ourselves whether
         * the text is longer than MAX_LINES, and ellipsize manually.
         */
        if (mNeedEllipsizeCheck) {
            if (getLayout() != null && mHeader != null) {
                if (getLayout().getLineCount() > MAX_LINES) {
                    int lineEndIndex = getLayout().getLineEnd(MAX_LINES - 1);
                    setText(getText().subSequence(0, lineEndIndex - 2) + ELLIPSIS);
                    showSubjectInMessageHeader();
                }
                mNeedEllipsizeCheck = false;
            }
        }
        super.onDraw(canvas);
    }

    public void setMessageHeader(final MessageHeader header) {
        mHeader = header;
    }
    
    public void showSubjectInMessageHeader() {
        mHeader.showSubjectLine();
    }
}
