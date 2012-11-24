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
package com.handmark.pulltorefresh.library;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Interpolator;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnPullEventListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.handmark.pulltorefresh.library.PullToRefreshBase.State;

public interface IPullToRefresh<T extends View> {

	/**
	 * Get the mode that this view is currently in. This is only really useful
	 * when using <code>Mode.BOTH</code>.
	 * 
	 * @return Mode that the view is currently in
	 */
	public Mode getCurrentMode();

	/**
	 * Returns whether the Touch Events are filtered or not. If true is
	 * returned, then the View will only use touch events where the difference
	 * in the Y-axis is greater than the difference in the X-axis. This means
	 * that the View will not interfere when it is used in a horizontal
	 * scrolling View (such as a ViewPager).
	 * 
	 * @return boolean - true if the View is filtering Touch Events
	 */
	public boolean getFilterTouchEvents();

	/**
	 * Get the mode that this view has been set to. If this returns
	 * <code>Mode.BOTH</code>, you can use <code>getCurrentMode()</code> to
	 * check which mode the view is currently in
	 * 
	 * @return Mode that the view has been set to
	 */
	public Mode getMode();

	/**
	 * Get the Wrapped Refreshable View. Anything returned here has already been
	 * added to the content view.
	 * 
	 * @return The View which is currently wrapped
	 */
	public T getRefreshableView();

	/**
	 * Get whether the 'Refreshing' View should be automatically shown when
	 * refreshing. Returns true by default.
	 * 
	 * @return - true if the Refreshing View will be show
	 */
	public boolean getShowViewWhileRefreshing();

	/**
	 * @return - The state that the View is currently in.
	 */
	public State getState();

	/**
	 * Returns whether the widget has disabled scrolling on the Refreshable View
	 * while refreshing.
	 * 
	 * @return true if the widget has disabled scrolling while refreshing
	 */
	public boolean isDisableScrollingWhileRefreshing();

	/**
	 * Gets whether Overscroll support is enabled. This is different to
	 * Android's standard Overscroll support (the edge-glow) which is available
	 * from GINGERBREAD onwards
	 * 
	 * @return true - if both PullToRefresh-OverScroll and Android's inbuilt
	 *         OverScroll are enabled
	 */
	public boolean isPullToRefreshOverScrollEnabled();

	/**
	 * Whether Pull-to-Refresh is enabled
	 * 
	 * @return enabled
	 */
	public boolean isPullToRefreshEnabled();

	/**
	 * Returns whether the Widget is currently in the Refreshing mState
	 * 
	 * @return true if the Widget is currently refreshing
	 */
	public boolean isRefreshing();

	/**
	 * Mark the current Refresh as complete. Will Reset the UI and hide the
	 * Refreshing View
	 */
	public void onRefreshComplete();

	/**
	 * By default the Widget disabled scrolling on the Refreshable View while
	 * refreshing. This method can change this behaviour.
	 * 
	 * @param disableScrollingWhileRefreshing
	 *            - true if you want to disable scrolling while refreshing
	 */
	public void setDisableScrollingWhileRefreshing(boolean disableScrollingWhileRefreshing);

	/**
	 * Set the Touch Events to be filtered or not. If set to true, then the View
	 * will only use touch events where the difference in the Y-axis is greater
	 * than the difference in the X-axis. This means that the View will not
	 * interfere when it is used in a horizontal scrolling View (such as a
	 * ViewPager), but will restrict which types of finger scrolls will trigger
	 * the View.
	 * 
	 * @param filterEvents
	 *            - true if you want to filter Touch Events. Default is true.
	 */
	public void setFilterTouchEvents(boolean filterEvents);

	/**
	 * Set the Last Updated Text. This displayed under the main label when
	 * Pulling
	 * 
	 * @param label
	 *            - Label to set
	 */
	public void setLastUpdatedLabel(CharSequence label);

	/**
	 * Set the drawable used in the loading layout. This is the same as calling
	 * <code>setLoadingDrawable(drawable, Mode.BOTH)</code>
	 * 
	 * @param drawable
	 *            - Drawable to display
	 */
	public void setLoadingDrawable(Drawable drawable);

	/**
	 * Set the drawable used in the loading layout.
	 * 
	 * @param drawable
	 *            - Drawable to display
	 * @param mode
	 *            - Controls which Header/Footer Views will be updated.
	 *            <code>Mode.BOTH</code> will update all available, other values
	 *            will update the relevant View.
	 */
	public void setLoadingDrawable(Drawable drawable, Mode mode);

	/**
	 * Set the mode of Pull-to-Refresh that this view will use.
	 * 
	 * @param mode
	 *            - Mode to set the View to
	 */
	public void setMode(Mode mode);

	/**
	 * Set OnPullEventListener for the Widget
	 * 
	 * @param listener
	 *            - Listener to be used when the Widget has a pull event to
	 *            propogate.
	 */
	public void setOnPullEventListener(OnPullEventListener<T> listener);

	/**
	 * Set OnRefreshListener for the Widget
	 * 
	 * @param listener
	 *            - Listener to be used when the Widget is set to Refresh
	 */
	public void setOnRefreshListener(OnRefreshListener<T> listener);

	/**
	 * Set OnRefreshListener for the Widget
	 * 
	 * @param listener
	 *            - Listener to be used when the Widget is set to Refresh
	 */
	public void setOnRefreshListener(OnRefreshListener2<T> listener);

	/**
	 * Sets whether Overscroll support is enabled. This is different to
	 * Android's standard Overscroll support (the edge-glow). This setting only
	 * takes effect when running on device with Android v2.3 or greater.
	 * 
	 * @param enabled
	 *            - true if you want Overscroll enabled
	 */
	public void setPullToRefreshOverScrollEnabled(boolean enabled);

	/**
	 * Set Text to show when the Widget is being Pulled
	 * <code>setPullLabel(releaseLabel, Mode.BOTH)</code>
	 * 
	 * @param releaseLabel
	 *            - CharSequence to display
	 */
	public void setPullLabel(CharSequence pullLabel);

	/**
	 * Set Text to show when the Widget is being Pulled
	 * 
	 * @param pullLabel
	 *            - CharSequence to display
	 * @param mode
	 *            - Controls which Header/Footer Views will be updated.
	 *            <code>Mode.BOTH</code> will update all available, other values
	 *            will update the relevant View.
	 */
	public void setPullLabel(CharSequence pullLabel, Mode mode);

	/**
	 * Sets the Widget to be in the refresh state. The UI will be updated to
	 * show the 'Refreshing' view, and be scrolled to show such.
	 */
	public void setRefreshing();

	/**
	 * Sets the Widget to be in the refresh state. The UI will be updated to
	 * show the 'Refreshing' view.
	 * 
	 * @param doScroll
	 *            - true if you want to force a scroll to the Refreshing view.
	 */
	public void setRefreshing(boolean doScroll);

	/**
	 * Set Text to show when the Widget is refreshing
	 * <code>setRefreshingLabel(releaseLabel, Mode.BOTH)</code>
	 * 
	 * @param releaseLabel
	 *            - CharSequence to display
	 */
	public void setRefreshingLabel(CharSequence refreshingLabel);

	/**
	 * Set Text to show when the Widget is refreshing
	 * 
	 * @param refreshingLabel
	 *            - CharSequence to display
	 * @param mode
	 *            - Controls which Header/Footer Views will be updated.
	 *            <code>Mode.BOTH</code> will update all available, other values
	 *            will update the relevant View.
	 */
	public void setRefreshingLabel(CharSequence refreshingLabel, Mode mode);

	/**
	 * Set Text to show when the Widget is being pulled, and will refresh when
	 * released. This is the same as calling
	 * <code>setReleaseLabel(releaseLabel, Mode.BOTH)</code>
	 * 
	 * @param releaseLabel
	 *            - CharSequence to display
	 */
	public void setReleaseLabel(CharSequence releaseLabel);

	/**
	 * Set Text to show when the Widget is being pulled, and will refresh when
	 * released
	 * 
	 * @param releaseLabel
	 *            - CharSequence to display
	 * @param mode
	 *            - Controls which Header/Footer Views will be updated.
	 *            <code>Mode.BOTH</code> will update all available, other values
	 *            will update the relevant View.
	 */
	public void setReleaseLabel(CharSequence releaseLabel, Mode mode);

	/**
	 * Sets the Animation Interpolator that is used for animated scrolling.
	 * Defaults to a DecelerateInterpolator
	 * 
	 * @param interpolator
	 *            - Interpolator to use
	 */
	public void setScrollAnimationInterpolator(Interpolator interpolator);

	/**
	 * A mutator to enable/disable whether the 'Refreshing' View should be
	 * automatically shown when refreshing.
	 * 
	 * @param showView
	 */
	public void setShowViewWhileRefreshing(boolean showView);

}