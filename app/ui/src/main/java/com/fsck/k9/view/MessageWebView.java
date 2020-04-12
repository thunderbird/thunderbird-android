package com.fsck.k9.view;


import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import timber.log.Timber;

import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.widget.Toast;

import com.fsck.k9.ui.R;
import com.fsck.k9.mailstore.AttachmentResolver;


public class MessageWebView extends WebView {

    private Context mContext;
    private OnSwipeLeftOrRightListener swipeLeftOrRightcallback;

    public MessageWebView(Context context) {
        super(context);
        mContext = context;
    }

    public MessageWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    /**
     * Configure a web view to load or not load network data. A <b>true</b> setting here means that
     * network data will be blocked.
     * @param shouldBlockNetworkData True if network data should be blocked, false to allow network data.
     */
    public void blockNetworkData(final boolean shouldBlockNetworkData) {
        /*
         * Block network loads.
         *
         * Images with content: URIs will not be blocked, nor
         * will network images that are already in the WebView cache.
         *
         */
        getSettings().setBlockNetworkLoads(shouldBlockNetworkData);
    }


    /**
     * Configure a {@link WebView} to display a Message. This method takes into account a user's
     * preferences when configuring the view. This message is used to view a message and to display a message being
     * replied to.
     */
    public void configure(WebViewConfig config) {
        this.setVerticalScrollBarEnabled(true);
        this.setVerticalScrollbarOverlay(true);
        this.setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
        this.setLongClickable(true);

        if (config.getUseDarkMode()) {
            // Black theme should get a black webview background
            // we'll set the background of the messages on load
            this.setBackgroundColor(0xff000000);
        }

        final WebSettings webSettings = this.getSettings();

        /* TODO this might improve rendering smoothness when webview is animated into view
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            webSettings.setOffscreenPreRaster(true);
        }
        */

        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        if (config.getAutoFitWidth()) {
            webSettings.setLoadWithOverviewMode(true);
        }

        disableDisplayZoomControls();

        webSettings.setJavaScriptEnabled(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setRenderPriority(RenderPriority.HIGH);

        // TODO:  Review alternatives.  NARROW_COLUMNS is deprecated on KITKAT
        webSettings.setLayoutAlgorithm(LayoutAlgorithm.NARROW_COLUMNS);

        setOverScrollMode(OVER_SCROLL_NEVER);

        webSettings.setTextZoom(config.getTextZoom());

        // Disable network images by default.  This is overridden by preferences.
        blockNetworkData(true);
    }

    public void setSwipeActionListener(OnSwipeLeftOrRightListener swipeCallback) {
        this.swipeLeftOrRightcallback = swipeCallback;
        // gesture to identify swipe left/right
        this.setOnTouchListener(new OnSwipeTouchListener(mContext) {
            public void onSwipeRight() {
                if(swipeLeftOrRightcallback != null) {
                    swipeLeftOrRightcallback.onSwipeRight();
                }
            }
            public void onSwipeLeft() {
                if(swipeLeftOrRightcallback != null) {
                    swipeLeftOrRightcallback.onSwipeLeft();
                }
            }
        });
    }

    /**
     * Detects left and right swipes across a view.
     */
    public class OnSwipeTouchListener implements View.OnTouchListener {

        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        public void onSwipeLeft() {
        }

        public void onSwipeRight() {
        }

        public boolean onTouch(View v, MotionEvent event) {
            gestureDetector.onTouchEvent(event);
            return v.onTouchEvent(event);
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_DISTANCE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;
            private Boolean canStillScrollRight = false;
            private Boolean canStillScrollLeft = false;

            @Override
            public boolean onDown(MotionEvent e) {
                canStillScrollLeft = canScrollHorizontally(+1);
                canStillScrollRight = canScrollHorizontally(-1);
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if(e1 == null || e2 == null)
                    return false;
                float distanceX = e2.getX() - e1.getX();
                float distanceY = e2.getY() - e1.getY();
                if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (distanceX > 0 && !canStillScrollRight)
                        onSwipeRight();
                    else if(distanceX < 0 && !canStillScrollLeft)
                        onSwipeLeft();
                    return true;
                }
                return false;
            }
        }
    }

    /**
     * Disable on-screen zoom controls on devices that support zooming via pinch-to-zoom.
     */
    private void disableDisplayZoomControls() {
        PackageManager pm = getContext().getPackageManager();
        boolean supportsMultiTouch =
                pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH) ||
                pm.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT);

        getSettings().setDisplayZoomControls(!supportsMultiTouch);
    }

    public void displayHtmlContentWithInlineAttachments(@NonNull String htmlText,
            @Nullable AttachmentResolver attachmentResolver, @Nullable OnPageFinishedListener onPageFinishedListener) {
        setWebViewClient(attachmentResolver, onPageFinishedListener);
        setHtmlContent(htmlText);
    }

    private void setWebViewClient(@Nullable AttachmentResolver attachmentResolver,
            @Nullable OnPageFinishedListener onPageFinishedListener) {
        K9WebViewClient webViewClient = K9WebViewClient.newInstance(attachmentResolver);
        if (onPageFinishedListener != null) {
            webViewClient.setOnPageFinishedListener(onPageFinishedListener);
        }
        setWebViewClient(webViewClient);
    }

    private void setHtmlContent(@NonNull String htmlText) {
        loadDataWithBaseURL("about:blank", htmlText, "text/html", "utf-8", null);
        resumeTimers();
    }

    /*
     * Emulate the shift key being pressed to trigger the text selection mode
     * of a WebView.
     */
    public void emulateShiftHeld() {
        try {

            KeyEvent shiftPressEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                                                    KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0);
            shiftPressEvent.dispatch(this, null, null);
            Toast.makeText(getContext() , R.string.select_text_now, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Timber.e(e, "Exception in emulateShiftHeld()");
        }
    }

    public interface OnPageFinishedListener {
        void onPageFinished();
    }

    public interface OnSwipeLeftOrRightListener {
        void onSwipeLeft();
        void onSwipeRight();
    }
}
