package com.fsck.k9.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ViewAnimator;

import com.fsck.k9.K9;

/**
 * A {@link ViewAnimator} that animates between two child views using different animations
 * depending on which view is displayed.
 */
public class ViewSwitcher extends ViewAnimator implements AnimationListener {
    private Animation firstInAnimation;
    private Animation firstOutAnimation;
    private Animation secondInAnimation;
    private Animation secondOutAnimation;
    private OnSwitchCompleteListener listener;


    public ViewSwitcher(Context context) {
        super(context);
    }

    public ViewSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void showFirstView() {
        if (getDisplayedChild() == 0) {
            return;
        }

        setupAnimations(firstInAnimation, firstOutAnimation);
        setDisplayedChild(0);
        handleSwitchCompleteCallback();
    }

    public void showSecondView() {
        if (getDisplayedChild() == 1) {
            return;
        }

        setupAnimations(secondInAnimation, secondOutAnimation);
        setDisplayedChild(1);
        handleSwitchCompleteCallback();
    }

    private void setupAnimations(Animation in, Animation out) {
        if (K9.showAnimations()) {
            setInAnimation(in);
            setOutAnimation(out);
            out.setAnimationListener(this);
        } else {
            setInAnimation(null);
            setOutAnimation(null);
        }
    }

    private void handleSwitchCompleteCallback() {
        if (!K9.showAnimations()) {
            onAnimationEnd(null);
        }
    }

    public Animation getFirstInAnimation() {
        return mFirstInAnimation;
    }

    public void setFirstInAnimation(Animation inAnimation) {
        this.firstInAnimation = inAnimation;
    }

    public Animation getFirstOutAnimation() {
        return firstOutAnimation;
    }

    public void setFirstOutAnimation(Animation outAnimation) {
        firstOutAnimation = outAnimation;
    }
    public Animation getSecondInAnimation() {
	 return secondInAnimation;
    }

    public void setSecondInAnimation(Animation inAnimation) {
        secondInAnimation = inAnimation;
    }

    public Animation getSecondOutAnimation() {
	 return secondOutAnimation;
    }

    public void setSecondOutAnimation(Animation outAnimation) {
        secondOutAnimation = outAnimation;
    }

    public void setOnSwitchCompleteListener(OnSwitchCompleteListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (listener != null) {
            listener.onSwitchComplete(getDisplayedChild());
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

    public interface OnSwitchCompleteListener {
        /**
         * This method will be called after the switch (including animation) has ended.
         *
         * @param displayedChild
         *         Contains the zero-based index of the child view that is now displayed.
         */
        void onSwitchComplete(int displayedChild);
    }
}
