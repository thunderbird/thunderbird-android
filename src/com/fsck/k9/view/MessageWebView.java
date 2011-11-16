package com.fsck.k9.view;

import android.content.Context;
import android.graphics.Picture;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingListener;

import java.lang.reflect.Method;
import java.util.Set;

public class MessageWebView extends WebView {
    // Store a reference to the listeners in MessagingController.  We can't fetch it directly since
    // we don't know the application name.
    private Set<MessagingListener> mListeners = null;

    /**
     * We use WebSettings.getBlockNetworkLoads() to prevent the WebView that displays email
     * bodies from loading external resources over the network. Unfortunately this method
     * isn't exposed via the official Android API. That's why we use reflection to be able
     * to call the method.
     */
    public static final Method mGetBlockNetworkLoads = K9.getMethod(WebSettings.class, "setBlockNetworkLoads");

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


        final WebSettings webSettings = this.getSettings();

        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setSupportZoom(true);
        webSettings.setJavaScriptEnabled(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        if (K9.zoomControlsEnabled()) {
            webSettings.setBuiltInZoomControls(true);
        }

        // SINGLE_COLUMN layout was broken on Android < 2.2, so we
        // administratively disable it
        if (Build.VERSION.SDK_INT > 7 && K9.mobileOptimizedLayout()) {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        } else {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        }

        webSettings.setTextSize(K9.getFontSizes().getMessageViewContent());

        // Disable network images by default.  This is overridden by preferences.
        blockNetworkData(true);

        // Listen for when the screen has finished drawing.
        setPictureListener(new PictureListener() {
            @Override
            public void onNewPicture(WebView webView, Picture picture) {
                if (mListeners != null) {
                    for (MessagingListener l : mListeners) {
                        l.messageViewFinished();
                    }
                }
            }
        });
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

    public void setListeners(final Set<MessagingListener> listeners) {
        this.mListeners = listeners;
    }
}
