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
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView.ScaleType;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.R;

public class RotateLoadingLayout extends LoadingLayout {

	static final int ROTATION_ANIMATION_DURATION = 1200;

	private final Animation mRotateAnimation;
	private final Matrix mHeaderImageMatrix;

	private float mRotationPivotX, mRotationPivotY;

	public RotateLoadingLayout(Context context, Mode mode, TypedArray attrs) {
		super(context, mode, attrs);

		mHeaderImage.setScaleType(ScaleType.MATRIX);
		mHeaderImageMatrix = new Matrix();
		mHeaderImage.setImageMatrix(mHeaderImageMatrix);

		mRotateAnimation = new RotateAnimation(0, 720, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		mRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
		mRotateAnimation.setDuration(ROTATION_ANIMATION_DURATION);
		mRotateAnimation.setRepeatCount(Animation.INFINITE);
		mRotateAnimation.setRepeatMode(Animation.RESTART);
	}

	public void onLoadingDrawableSet(Drawable imageDrawable) {
		if (null != imageDrawable) {
			mRotationPivotX = imageDrawable.getIntrinsicWidth() / 2f;
			mRotationPivotY = imageDrawable.getIntrinsicHeight() / 2f;
		}
	}

	protected void onPullYImpl(float scaleOfHeight) {
		mHeaderImageMatrix.setRotate(scaleOfHeight * 90, mRotationPivotX, mRotationPivotY);
		mHeaderImage.setImageMatrix(mHeaderImageMatrix);
	}

	@Override
	protected void refreshingImpl() {
		mHeaderImage.startAnimation(mRotateAnimation);
	}

	@Override
	protected void resetImpl() {
		mHeaderImage.clearAnimation();
		resetImageRotation();
	}

	private void resetImageRotation() {
		if (null != mHeaderImageMatrix) {
			mHeaderImageMatrix.reset();
			mHeaderImage.setImageMatrix(mHeaderImageMatrix);
		}
	}

	@Override
	protected void pullToRefreshImpl() {
		// NO-OP
	}

	@Override
	protected void releaseToRefreshImpl() {
		// NO-OP
	}

	@Override
	protected int getDefaultTopDrawableResId() {
		return R.drawable.default_ptr_rotate;
	}

	@Override
	protected int getDefaultBottomDrawableResId() {
		return R.drawable.default_ptr_rotate;
	}

}
