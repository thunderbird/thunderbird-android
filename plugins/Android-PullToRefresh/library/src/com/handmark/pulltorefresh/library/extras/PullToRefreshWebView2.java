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
package com.handmark.pulltorefresh.library.extras;

import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

import com.handmark.pulltorefresh.library.PullToRefreshWebView;

/**
 * An advanced version of {@link PullToRefreshWebView} which delegates the
 * triggering of the PullToRefresh gesture to the Javascript running within the
 * WebView. This means that you should only use this class if:
 * 
 * <ul>
 * <li>{@link PullToRefreshWebView} doesn't work correctly because you're using
 * <code>overflow:scroll</code> or something else which means
 * {@link WebView#getScrollY()} doesn't return correct values.</li>
 * <li>You control the web content being displayed, as you need to write some
 * Javascript callbacks.</li>
 * </ul>
 * <p />
 * 
 * The way this call works is that when a PullToRefresh gesture is in action,
 * the following Javascript methods will be called:
 * <code>isReadyForPullDown()</code> and <code>isReadyForPullUp()</code>, it is
 * your job to calculate whether the view is in a state where a PullToRefresh
 * can happen, and return the result via the callback mechanism. An example can
 * be seen below:
 * 
 * <pre>
 * function isReadyForPullDown() {
 *   var result = ...  // Probably using the .scrollTop DOM attribute
 *   ptr.isReadyForPullDownResponse(result);
 * }
 * 
 * function isReadyForPullUp() {
 *   var result = ...  // Probably using the .scrollBottom DOM attribute
 *   ptr.isReadyForPullUpResponse(result);
 * }
 * </pre>
 * 
 * @author Chris Banes
 */
public class PullToRefreshWebView2 extends PullToRefreshWebView {

	static final String JS_INTERFACE_PKG = "ptr";
	static final String DEF_JS_READY_PULL_DOWN_CALL = "javascript:isReadyForPullDown();";
	static final String DEF_JS_READY_PULL_UP_CALL = "javascript:isReadyForPullUp();";

	public PullToRefreshWebView2(Context context) {
		super(context);
	}

	public PullToRefreshWebView2(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PullToRefreshWebView2(Context context, Mode mode) {
		super(context, mode);
	}

	private JsValueCallback mJsCallback;
	private final AtomicBoolean mIsReadyForPullDown = new AtomicBoolean(false);
	private final AtomicBoolean mIsReadyForPullUp = new AtomicBoolean(false);

	@Override
	protected WebView createRefreshableView(Context context, AttributeSet attrs) {
		WebView webView = super.createRefreshableView(context, attrs);

		// Need to add JS Interface so we can get the response back
		mJsCallback = new JsValueCallback();
		webView.addJavascriptInterface(mJsCallback, JS_INTERFACE_PKG);

		return webView;
	}

	@Override
	protected boolean isReadyForPullDown() {
		// Call Javascript...
		getRefreshableView().loadUrl(DEF_JS_READY_PULL_DOWN_CALL);

		// Response will be given to JsValueCallback, which will update
		// mIsReadyForPullDown

		return mIsReadyForPullDown.get();
	}

	@Override
	protected boolean isReadyForPullUp() {
		// Call Javascript...
		getRefreshableView().loadUrl(DEF_JS_READY_PULL_UP_CALL);

		// Response will be given to JsValueCallback, which will update
		// mIsReadyForPullUp

		return mIsReadyForPullUp.get();
	}

	/**
	 * Used for response from Javascript
	 * 
	 * @author Chris Banes
	 */
	final class JsValueCallback {

		public void isReadyForPullUpResponse(boolean response) {
			mIsReadyForPullUp.set(response);
		}

		public void isReadyForPullDownResponse(boolean response) {
			mIsReadyForPullDown.set(response);
		}
	}
}
