package com.fsck.k9.view;


import app.k9mail.legacy.di.DI;
import com.fsck.k9.K9;
import net.thunderbird.core.preferences.GeneralSettingsManager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ViewAnimator;


/**
 * A {@link ViewAnimator} that animates between two child views using different animations depending on which view is
 * displayed.
 */
public class ViewSwitcher extends ViewAnimator implements AnimationListener {
    private Animation mFirstInAnimation;
    private Animation mFirstOutAnimation;
    private Animation mSecondInAnimation;
    private Animation mSecondOutAnimation;
    private OnSwitchCompleteListener mListener;

    private GeneralSettingsManager generalSettingsManager = DI.get(GeneralSettingsManager.class);


    public ViewSwitcher(Context context) {
        super(context);
    }

    public ViewSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void showFirstView() {
        if (getDisplayedChild() == 0) {
            onAnimationEnd(null);
            return;
        }

        setupAnimations(mFirstInAnimation, mFirstOutAnimation);
        setDisplayedChild(0);
        handleSwitchCompleteCallback();
    }

    public void showSecondView() {
        if (getDisplayedChild() == 1) {
            onAnimationEnd(null);
            return;
        }

        setupAnimations(mSecondInAnimation, mSecondOutAnimation);
        setDisplayedChild(1);
        handleSwitchCompleteCallback();
    }

    private void setupAnimations(Animation in, Animation out) {
        if (generalSettingsManager.getSettings().isShowAnimations()) {
            setInAnimation(in);
            setOutAnimation(out);
            out.setAnimationListener(this);
        } else {
            setInAnimation(null);
            setOutAnimation(null);
        }
    }

    private void handleSwitchCompleteCallback() {
        if (!generalSettingsManager.getSettings().isShowAnimations()) {
            onAnimationEnd(null);
        }
    }

    public Animation getFirstInAnimation() {
        return mFirstInAnimation;
    }

    public void setFirstInAnimation(Animation inAnimation) {
        this.mFirstInAnimation = inAnimation;
    }

    public Animation getFirstOutAnimation() {
        return mFirstOutAnimation;
    }

    public void setFirstOutAnimation(Animation outAnimation) {
        mFirstOutAnimation = outAnimation;
    }

    public Animation getSecondInAnimation() {
        return mSecondInAnimation;
    }

    public void setSecondInAnimation(Animation inAnimation) {
        mSecondInAnimation = inAnimation;
    }

    public Animation getSecondOutAnimation() {
        return mSecondOutAnimation;
    }

    public void setSecondOutAnimation(Animation outAnimation) {
        mSecondOutAnimation = outAnimation;
    }

    public void setOnSwitchCompleteListener(OnSwitchCompleteListener listener) {
        mListener = listener;
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (mListener != null) {
            mListener.onSwitchComplete(getDisplayedChild());
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        // unused
    }

    @Override
    public void onAnimationStart(Animation animation) {
        // unused
    }

    public GeneralSettingsManager getGeneralSettingsManager() {
        return generalSettingsManager;
    }

    public void setGeneralSettingsManager(GeneralSettingsManager generalSettingsManager) {
        this.generalSettingsManager = generalSettingsManager;
    }

    public interface OnSwitchCompleteListener {
        /**
         * This method will be called after the switch (including animation) has ended.
         *
         * @param displayedChild Contains the zero-based index of the child view that is now displayed.
         */
        void onSwitchComplete(int displayedChild);
    }
}
