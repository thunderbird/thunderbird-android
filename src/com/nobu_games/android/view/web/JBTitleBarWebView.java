package com.nobu_games.android.view.web;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebViewClassic.TitleBarDelegate;

public class JBTitleBarWebView extends TitleBarWebView implements
        TitleBarDelegate {

    public JBTitleBarWebView(Context context) {
        super(context);
    }

    public JBTitleBarWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public JBTitleBarWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * <i>Makes sure that the title bar view gets touch events</i>
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(mTitleBar != null) {
            final int sy = getScrollY();
            final int visTitleHeight = getVisibleTitleHeightCompat();
            final float x = event.getX();
            float y = event.getY();

            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if(y <= visTitleHeight) {
                        mTouchInTitleBar = true;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    mTouchMove = true;
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mTouchMove = false;
                    break;

                default:
            }

            if(mTouchInTitleBar) {
                y += sy;
                event.setLocation(x, y);

                return mTitleBar.dispatchTouchEvent(event);
            }
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    public int getTitleHeight() {
        if(mTitleBar != null) return mTitleBar.getHeight();
        return 0;
    }

    @Override
    public void onSetEmbeddedTitleBar(View title) {
    }

}
