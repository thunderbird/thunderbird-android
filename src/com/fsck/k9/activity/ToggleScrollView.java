package com.fsck.k9.activity;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class ToggleScrollView extends ScrollView
{
    private boolean mScrolling = true;

    public ToggleScrollView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public void setScrolling(boolean enable)
    {
        mScrolling = enable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        return (mScrolling) ? super.onTouchEvent(ev) : true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        return (mScrolling) ? super.onInterceptTouchEvent(ev) : false;
    }
}
