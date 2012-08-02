package com.fsck.k9.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import java.lang.reflect.Method;
import com.nobu_games.android.view.web.TitleBarWebView;
public class MessageWebView extends TitleBarWebView {


    /**
     * We use WebSettings.getBlockNetworkLoads() to prevent the WebView that displays email
     * bodies from loading external resources over the network. Unfortunately this method
     * isn't exposed via the official Android API. That's why we use reflection to be able
     * to call the method.
     */
    public static final Method mGetBlockNetworkLoads = K9.getMethod(WebSettings.class, "setBlockNetworkLoads");

    /**
     * Check whether the single column layout algorithm can be used on this version of Android.
     *
     * <p>
     * Single column layout was broken on Android < 2.2 (see
     * <a href="http://code.google.com/p/android/issues/detail?id=5024">issue 5024</a>).
     * </p>
     *
     * <p>
     * Android versions >= 3.0 have problems with unclickable links when single column layout is
     * enabled (see
     * <a href="http://code.google.com/p/android/issues/detail?id=34886">issue 34886</a>
     * in Android's bug tracker, and
     * <a href="http://code.google.com/p/k9mail/issues/detail?id=3820">issue 3820</a>
     * in K-9 Mail's bug tracker).
     */
    public static boolean isSingleColumnLayoutSupported() {
        return (Build.VERSION.SDK_INT > 7 && Build.VERSION.SDK_INT < 11);
    }


    public MessageWebView(Context context) {
        super(context);
    }

    public MessageWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Configure a web view to load or not load network data. A <b>true</b> setting here means that
     * network data will be blocked.
     * @param shouldBlockNetworkData True if network data should be blocked, false to allow network data.
     */
    public void blockNetworkData(final boolean shouldBlockNetworkData) {
        // Sanity check to make sure we don't blow up.
        if (getSettings() == null) {
            return;
        }

        // Block network loads.
        if (mGetBlockNetworkLoads != null) {
            try {
                mGetBlockNetworkLoads.invoke(getSettings(), shouldBlockNetworkData);
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Error on invoking WebSettings.setBlockNetworkLoads()", e);
            }
        }

        // Block network images.
        getSettings().setBlockNetworkImage(shouldBlockNetworkData);
    }


    /**
     * Configure a {@link android.webkit.WebView} to display a Message. This method takes into account a user's
     * preferences when configuring the view. This message is used to view a message and to display a message being
     * replied to.
     */
    public void configure() {
        this.setVerticalScrollBarEnabled(true);
        this.setVerticalScrollbarOverlay(true);
        this.setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
        this.setLongClickable(true);

        if (K9.getK9Theme() == K9.THEME_DARK) {
            // Black theme should get a black webview background
            // we'll set the background of the messages on load
            this.setBackgroundColor(0xff000000);
        }

        final WebSettings webSettings = this.getSettings();

        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setSupportZoom(true);
        webSettings.setJavaScriptEnabled(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        if (K9.zoomControlsEnabled()) {
            webSettings.setBuiltInZoomControls(true);
        }

        if (isSingleColumnLayoutSupported() && K9.mobileOptimizedLayout()) {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        } else {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        }

        disableOverscrolling();

        webSettings.setTextSize(K9.getFontSizes().getMessageViewContent());

        // Disable network images by default.  This is overridden by preferences.
        blockNetworkData(true);

    }

    @TargetApi(9)
    private void disableOverscrolling() {
        if (Build.VERSION.SDK_INT >= 9) {
            setOverScrollMode(OVER_SCROLL_NEVER);
        }
    }

    public void setText(String text, String contentType) {
        String content = text;
        if (K9.getK9Theme() == K9.THEME_DARK)  {
            // It's a little wrong to just throw in the <style> before the opening <html>
            // but it's less wrong than trying to edit the html stream
            content = "<style>* { background: black ! important; color: white !important }" +
                   ":link, :link * { color: #CCFF33 !important }" +
                   ":visited, :visited * { color: #551A8B !important }</style> "
                   + content;
        }
        loadDataWithBaseURL("http://", content, contentType, "utf-8", null);
    }

    /*
     * Emulate the shift key being pressed to trigger the text selection mode
     * of a WebView.
     */
    @Override
    public void emulateShiftHeld() {
        try {

            KeyEvent shiftPressEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                                                    KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0);
            shiftPressEvent.dispatch(this);
            Toast.makeText(getContext() , R.string.select_text_now, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Exception in emulateShiftHeld()", e);
        }
    }

}
