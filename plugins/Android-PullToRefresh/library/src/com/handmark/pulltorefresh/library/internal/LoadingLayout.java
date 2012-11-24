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
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.R;

public abstract class LoadingLayout extends LinearLayout {

	static final Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();

	protected final ImageView mHeaderImage;
	protected final ProgressBar mHeaderProgress;

	private boolean mUseIntrinisicAnimation;

	private final TextView mHeaderText;
	private final TextView mSubHeaderText;

	private CharSequence mPullLabel;
	private CharSequence mRefreshingLabel;
	private CharSequence mReleaseLabel;

	public LoadingLayout(Context context, final Mode mode, TypedArray attrs) {
		super(context);

		setGravity(Gravity.CENTER_VERTICAL);

		final int tbPadding = getResources().getDimensionPixelSize(R.dimen.header_footer_top_bottom_padding);
		final int lrPadding = getResources().getDimensionPixelSize(R.dimen.header_footer_left_right_padding);
		setPadding(lrPadding, tbPadding, lrPadding, tbPadding);

		LayoutInflater.from(context).inflate(R.layout.pull_to_refresh_header, this);
		mHeaderText = (TextView) findViewById(R.id.pull_to_refresh_text);
		mHeaderProgress = (ProgressBar) findViewById(R.id.pull_to_refresh_progress);
		mSubHeaderText = (TextView) findViewById(R.id.pull_to_refresh_sub_text);
		mHeaderImage = (ImageView) findViewById(R.id.pull_to_refresh_image);

		switch (mode) {
			case PULL_UP_TO_REFRESH:
				// Load in labels
				mPullLabel = context.getString(R.string.pull_to_refresh_from_bottom_pull_label);
				mRefreshingLabel = context.getString(R.string.pull_to_refresh_from_bottom_refreshing_label);
				mReleaseLabel = context.getString(R.string.pull_to_refresh_from_bottom_release_label);
				break;

			case PULL_DOWN_TO_REFRESH:
			default:
				// Load in labels
				mPullLabel = context.getString(R.string.pull_to_refresh_pull_label);
				mRefreshingLabel = context.getString(R.string.pull_to_refresh_refreshing_label);
				mReleaseLabel = context.getString(R.string.pull_to_refresh_release_label);
				break;
		}

		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderTextColor)) {
			ColorStateList colors = attrs.getColorStateList(R.styleable.PullToRefresh_ptrHeaderTextColor);
			setTextColor(null != colors ? colors : ColorStateList.valueOf(Color.BLACK));
		}
		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderSubTextColor)) {
			ColorStateList colors = attrs.getColorStateList(R.styleable.PullToRefresh_ptrHeaderSubTextColor);
			setSubTextColor(null != colors ? colors : ColorStateList.valueOf(Color.BLACK));
		}
		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderBackground)) {
			Drawable background = attrs.getDrawable(R.styleable.PullToRefresh_ptrHeaderBackground);
			if (null != background) {
				setBackgroundDrawable(background);
			}
		}

		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderTextAppearance)) {
			TypedValue styleID = new TypedValue();
			attrs.getValue(R.styleable.PullToRefresh_ptrHeaderTextAppearance, styleID);
			setTextAppearance(styleID.data);
		}
		if (attrs.hasValue(R.styleable.PullToRefresh_ptrSubHeaderTextAppearance)) {
			TypedValue styleID = new TypedValue();
			attrs.getValue(R.styleable.PullToRefresh_ptrSubHeaderTextAppearance, styleID);
			setSubTextAppearance(styleID.data);
		}

		// Try and get defined drawable from Attrs
		Drawable imageDrawable = null;
		if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawable)) {
			imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawable);
		}

		// Check Specific Drawable from Attrs, these overrite the generic
		// drawable attr above
		if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawableTop) && mode == Mode.PULL_DOWN_TO_REFRESH) {
			imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawableTop);
		} else if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawableBottom) && mode == Mode.PULL_UP_TO_REFRESH) {
			imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawableTop);
		}

		// If we don't have a user defined drawable, load the default
		if (null == imageDrawable) {
			if (mode == Mode.PULL_DOWN_TO_REFRESH) {
				imageDrawable = context.getResources().getDrawable(getDefaultBottomDrawableResId());
			} else {
				imageDrawable = context.getResources().getDrawable(getDefaultTopDrawableResId());
			}
		}

		// Set Drawable, and save width/height
		setLoadingDrawable(imageDrawable);

		reset();
	}

	public final void onPullY(float scaleOfHeight) {
		if (!mUseIntrinisicAnimation) {
			onPullYImpl(scaleOfHeight);
		}
	}

	public final void pullToRefresh() {
		mHeaderText.setText(mPullLabel);

		// Now call the callback
		pullToRefreshImpl();
	}

	public final void refreshing() {
		mHeaderText.setText(mRefreshingLabel);

		if (mUseIntrinisicAnimation) {
			((AnimationDrawable) mHeaderImage.getDrawable()).start();
		} else {
			// Now call the callback
			refreshingImpl();
		}

		mSubHeaderText.setVisibility(View.GONE);
	}

	public final void releaseToRefresh() {
		mHeaderText.setText(mReleaseLabel);

		// Now call the callback
		releaseToRefreshImpl();
	}

	public final void reset() {
		mHeaderText.setText(mPullLabel);
		mHeaderImage.setVisibility(View.VISIBLE);

		if (mUseIntrinisicAnimation) {
			((AnimationDrawable) mHeaderImage.getDrawable()).stop();
		} else {
			// Now call the callback
			resetImpl();
		}

		if (TextUtils.isEmpty(mSubHeaderText.getText())) {
			mSubHeaderText.setVisibility(View.GONE);
		} else {
			mSubHeaderText.setVisibility(View.VISIBLE);
		}
	}

	public final void setLoadingDrawable(Drawable imageDrawable) {
		// Set Drawable
		mHeaderImage.setImageDrawable(imageDrawable);
		mUseIntrinisicAnimation = (imageDrawable instanceof AnimationDrawable);

		// Now call the callback
		onLoadingDrawableSet(imageDrawable);
	}

	public void setPullLabel(CharSequence pullLabel) {
		mPullLabel = pullLabel;
	}

	public void setRefreshingLabel(CharSequence refreshingLabel) {
		mRefreshingLabel = refreshingLabel;
	}

	public void setReleaseLabel(CharSequence releaseLabel) {
		mReleaseLabel = releaseLabel;
	}

	public void setSubHeaderText(CharSequence label) {
		if (TextUtils.isEmpty(label)) {
			mSubHeaderText.setVisibility(View.GONE);
		} else {
			mSubHeaderText.setText(label);
			mSubHeaderText.setVisibility(View.VISIBLE);
		}
	}

	public void setSubTextAppearance(int value) {
		mSubHeaderText.setTextAppearance(getContext(), value);
	}

	public void setSubTextColor(ColorStateList color) {
		mSubHeaderText.setTextColor(color);
	}

	public void setSubTextColor(int color) {
		setSubTextColor(ColorStateList.valueOf(color));
	}

	public void setTextAppearance(int value) {
		mHeaderText.setTextAppearance(getContext(), value);
		mSubHeaderText.setTextAppearance(getContext(), value);
	}

	public void setTextColor(ColorStateList color) {
		mHeaderText.setTextColor(color);
		mSubHeaderText.setTextColor(color);
	}

	public void setTextColor(int color) {
		setTextColor(ColorStateList.valueOf(color));
	}

	/**
	 * Callbacks for derivative Layouts
	 */

	protected abstract int getDefaultBottomDrawableResId();

	protected abstract int getDefaultTopDrawableResId();

	protected abstract void onLoadingDrawableSet(Drawable imageDrawable);

	protected abstract void onPullYImpl(float scaleOfHeight);

	protected abstract void pullToRefreshImpl();

	protected abstract void refreshingImpl();

	protected abstract void releaseToRefreshImpl();

	protected abstract void resetImpl();

}
