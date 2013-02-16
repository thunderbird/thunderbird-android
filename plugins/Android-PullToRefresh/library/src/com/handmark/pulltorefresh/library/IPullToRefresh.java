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

import android.view.View;
import android.view.animation.Interpolator;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnPullEventListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.handmark.pulltorefresh.library.PullToRefreshBase.State;

public interface IPullToRefresh<T extends View> {

	/**
	 * Demos the Pull-to-Refresh functionality to the user so that they are
	 * aware it is there. This could be useful when the user first opens your
	 * app, etc. The animation will only happen if the Refresh View (ListView,
	 * ScrollView, etc) is in a state where a Pull-to-Refresh could occur by a
	 * user's touch gesture (i.e. scrolled to the top/bottom).
	 * 
	 * @return true - if the Demo has been started, false if not.
	 */
	public boolean demo();

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
	 * Returns a proxy object which allows you to call methods on all of the
	 * LoadingLayouts (the Views which show when Pulling/Refreshing).
	 * <p />
	 * You should not keep the result of this method any longer than you need
	 * it.
	 * 
	 * @return Object which will proxy any calls you make on it, to all of the
	 *         LoadingLayouts.
	 */
	public ILoadingLayout getLoadingLayoutProxy();

	/**
	 * Returns a proxy object which allows you to call methods on the
	 * LoadingLayouts (the Views which show when Pulling/Refreshing). The actual
	 * LoadingLayout(s) which will be affected, are chosen by the parameters you
	 * give.
	 * <p />
	 * You should not keep the result of this method any longer than you need
	 * it.
	 * 
	 * @param includeStart - Whether to include the Start/Header Views
	 * @param includeEnd - Whether to include the End/Footer Views
	 * @return Object which will proxy any calls you make on it, to the
	 *         LoadingLayouts included.
	 */
	public ILoadingLayout getLoadingLayoutProxy(boolean includeStart, boolean includeEnd);

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
	 * Whether Pull-to-Refresh is enabled
	 * 
	 * @return enabled
	 */
	public boolean isPullToRefreshEnabled();

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
	 * Returns whether the Widget is currently in the Refreshing mState
	 * 
	 * @return true if the Widget is currently refreshing
	 */
	public boolean isRefreshing();

	/**
	 * Returns whether the widget has enabled scrolling on the Refreshable View
	 * while refreshing.
	 * 
	 * @return true if the widget has enabled scrolling while refreshing
	 */
	public boolean isScrollingWhileRefreshingEnabled();

	/**
	 * Mark the current Refresh as complete. Will Reset the UI and hide the
	 * Refreshing View
	 */
	public void onRefreshComplete();

	/**
	 * Set the Touch Events to be filtered or not. If set to true, then the View
	 * will only use touch events where the difference in the Y-axis is greater
	 * than the difference in the X-axis. This means that the View will not
	 * interfere when it is used in a horizontal scrolling View (such as a
	 * ViewPager), but will restrict which types of finger scrolls will trigger
	 * the View.
	 * 
	 * @param filterEvents - true if you want to filter Touch Events. Default is
	 *            true.
	 */
	public void setFilterTouchEvents(boolean filterEvents);

	/**
	 * Set the mode of Pull-to-Refresh that this view will use.
	 * 
	 * @param mode - Mode to set the View to
	 */
	public void setMode(Mode mode);

	/**
	 * Set OnPullEventListener for the Widget
	 * 
	 * @param listener - Listener to be used when the Widget has a pull event to
	 *            propogate.
	 */
	public void setOnPullEventListener(OnPullEventListener<T> listener);

	/**
	 * Set OnRefreshListener for the Widget
	 * 
	 * @param listener - Listener to be used when the Widget is set to Refresh
	 */
	public void setOnRefreshListener(OnRefreshListener<T> listener);

	/**
	 * Set OnRefreshListener for the Widget
	 * 
	 * @param listener - Listener to be used when the Widget is set to Refresh
	 */
	public void setOnRefreshListener(OnRefreshListener2<T> listener);

	/**
	 * Sets whether Overscroll support is enabled. This is different to
	 * Android's standard Overscroll support (the edge-glow). This setting only
	 * takes effect when running on device with Android v2.3 or greater.
	 * 
	 * @param enabled - true if you want Overscroll enabled
	 */
	public void setPullToRefreshOverScrollEnabled(boolean enabled);

	/**
	 * Sets the Widget to be in the refresh state. The UI will be updated to
	 * show the 'Refreshing' view, and be scrolled to show such.
	 */
	public void setRefreshing();

	/**
	 * Sets the Widget to be in the refresh state. The UI will be updated to
	 * show the 'Refreshing' view.
	 * 
	 * @param doScroll - true if you want to force a scroll to the Refreshing
	 *            view.
	 */
	public void setRefreshing(boolean doScroll);

	/**
	 * Sets the Animation Interpolator that is used for animated scrolling.
	 * Defaults to a DecelerateInterpolator
	 * 
	 * @param interpolator - Interpolator to use
	 */
	public void setScrollAnimationInterpolator(Interpolator interpolator);

	/**
	 * By default the Widget disables scrolling on the Refreshable View while
	 * refreshing. This method can change this behaviour.
	 * 
	 * @param scrollingWhileRefreshingEnabled - true if you want to enable
	 *            scrolling while refreshing
	 */
	public void setScrollingWhileRefreshingEnabled(boolean scrollingWhileRefreshingEnabled);

	/**
	 * A mutator to enable/disable whether the 'Refreshing' View should be
	 * automatically shown when refreshing.
	 * 
	 * @param showView
	 */
	public void setShowViewWhileRefreshing(boolean showView);

}