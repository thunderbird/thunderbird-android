/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.handmark.pulltorefresh.library.internal;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.R;

public class FlipLoadingLayout extends LoadingLayout {

	static final int FLIP_ANIMATION_DURATION = 150;

	private final Animation mRotateAnimation, mResetRotateAnimation;

	public FlipLoadingLayout(Context context, Mode mode, TypedArray attrs) {
		super(context, mode, attrs);
		
		final int rotateAngle = mode == Mode.PULL_DOWN_TO_REFRESH ? 180 : -180;

		mRotateAnimation = new RotateAnimation(0, rotateAngle, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		mRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
		mRotateAnimation.setDuration(FLIP_ANIMATION_DURATION);
		mRotateAnimation.setFillAfter(true);

		mResetRotateAnimation = new RotateAnimation(rotateAngle, 0, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		mResetRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
		mResetRotateAnimation.setDuration(FLIP_ANIMATION_DURATION);
		mResetRotateAnimation.setFillAfter(true);
	}

	@Override
	protected void onLoadingDrawableSet(Drawable imageDrawable) {
		/**
		 * We need to set the width/height of the ImageView so that it is square
		 * with each side the size of the largest drawable dimension. This is so
		 * that it doesn't clip when rotated.
		 */
		if (null != imageDrawable) {
			ViewGroup.LayoutParams lp = mHeaderImage.getLayoutParams();
			lp.width = lp.height = Math.max(imageDrawable.getIntrinsicWidth(), imageDrawable.getIntrinsicHeight());
			mHeaderImage.requestLayout();
		}
	}

	@Override
	protected void onPullYImpl(float scaleOfHeight) {
		// NO-OP
	}

	@Override
	protected void pullToRefreshImpl() {
		// Only start reset Animation, we've previously show the rotate anim
		if (mRotateAnimation == mHeaderImage.getAnimation()) {
			mHeaderImage.startAnimation(mResetRotateAnimation);
		}
	}

	@Override
	protected void refreshingImpl() {
		mHeaderImage.clearAnimation();
		mHeaderImage.setVisibility(View.INVISIBLE);
		mHeaderProgress.setVisibility(View.VISIBLE);
	}

	@Override
	protected void releaseToRefreshImpl() {
		mHeaderImage.startAnimation(mRotateAnimation);
	}

	@Override
	protected void resetImpl() {
		mHeaderImage.clearAnimation();
		mHeaderProgress.setVisibility(View.GONE);
		mHeaderImage.setVisibility(View.VISIBLE);
	}

	@Override
	protected int getDefaultTopDrawableResId() {
		return R.drawable.default_ptr_flip_top;
	}

	@Override
	protected int getDefaultBottomDrawableResId() {
		return R.drawable.default_ptr_flip_bottom;
	}

}
