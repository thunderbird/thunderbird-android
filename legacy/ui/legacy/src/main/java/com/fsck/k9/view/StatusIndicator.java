package com.fsck.k9.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.AnimationUtils;

import com.fsck.k9.ui.R;


public class StatusIndicator extends ToolableViewAnimator {

    public enum Status {
        IDLE, PROGRESS, OK, ERROR
    }

    public StatusIndicator(Context context) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.status_indicator, this, true);
        setInAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in));
        setOutAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));
    }

    public StatusIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.status_indicator, this, true);
        setInAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in));
        setOutAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));
    }

    @Override
    public void setDisplayedChild(int whichChild) {
        if (whichChild != getDisplayedChild()) {
            super.setDisplayedChild(whichChild);
        }
    }

    public void setDisplayedChild(Status status) {
        setDisplayedChild(status.ordinal());
    }

}
