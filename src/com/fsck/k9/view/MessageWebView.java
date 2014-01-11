package com.fsck.k9.view;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Browser;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.provider.AttachmentProvider;

public class MessageWebView extends RigidWebView {


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
        WebSettings webSettings = getSettings();

        // Block network loads.
        webSettings.setBlockNetworkLoads(shouldBlockNetworkData);

        // Block network images.
        webSettings.setBlockNetworkImage(shouldBlockNetworkData);
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

        if (K9.getK9MessageViewTheme() == K9.Theme.DARK) {
            // Black theme should get a black webview background
            // we'll set the background of the messages on load
            this.setBackgroundColor(0xff000000);
        }

        setWebViewClient(new LocalWebViewClient());

        final WebSettings webSettings = this.getSettings();

        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        if (K9.autofitWidth()) {
            webSettings.setLoadWithOverviewMode(true);
        }

        disableDisplayZoomControls();

        webSettings.setJavaScriptEnabled(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

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

    /**
     * Disable on-screen zoom controls on devices that support zooming via pinch-to-zoom.
     */
    @TargetApi(11)
    private void disableDisplayZoomControls() {
        if (Build.VERSION.SDK_INT >= 11) {
            PackageManager pm = getContext().getPackageManager();
            boolean supportsMultiTouch =
                    pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH) ||
                    pm.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT);

            getSettings().setDisplayZoomControls(!supportsMultiTouch);
        }
    }

    @TargetApi(9)
    private void disableOverscrolling() {
        if (Build.VERSION.SDK_INT >= 9) {
            setOverScrollMode(OVER_SCROLL_NEVER);
        }
    }

    /**
     * Load a message body into a {@code MessageWebView}
     *
     * <p>
     * Before loading, the text is wrapped in an HTML header and footer
     * so that it displays properly.
     * </p>
     *
     * @param text
     *      The message body to display.  Assumed to be MIME type text/html.
     */
    public void setText(String text) {
     // Include a meta tag so the WebView will not use a fixed viewport width of 980 px
        String content = "<html><head><meta name=\"viewport\" content=\"width=device-width\"/>";
        if (K9.getK9MessageViewTheme() == K9.Theme.DARK)  {
            content += "<style type=\"text/css\">" +
                   "* { background: black ! important; color: #F3F3F3 !important }" +
                   ":link, :link * { color: #CCFF33 !important }" +
                   ":visited, :visited * { color: #551A8B !important }</style> ";
        }
        content += HtmlConverter.cssStylePre();
        content += "</head><body>" + text + "</body></html>";
        loadDataWithBaseURL("http://", content, "text/html", "utf-8", null);
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
            Log.e(K9.LOG_TAG, "Exception in emulateShiftHeld()", e);
        }
    }

    private class LocalWebViewClient extends WebViewClient {

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                String url) {
            Uri uri = Uri.parse(url);
            if (AttachmentProvider.CID_SCHEME.equals(uri.getScheme())) {
                Uri contentUri = AttachmentProvider.convertToContentUri(uri);
                ContentResolver contentResolver = getContext()
                        .getContentResolver();
                InputStream inputStream;
                try {
                    inputStream = contentResolver.openInputStream(contentUri);
                } catch (FileNotFoundException e) {
                    return null;
                }
                String mimeType = contentResolver.getType(contentUri);
                return new WebResourceResponse(mimeType, null, inputStream);
            } else {
                return null;
            }
        }

        /*
         * This replicates the default behavior that would occur if no
         * WebViewClient had been set for the WebView, i.e., as if:
         *
         * WebView.setWebViewClient(null);
         *
         * Without this, web links in message bodies fail to load when
         * tapped.
         *
         * Code source:
         * https://android.googlesource.com/platform/frameworks/webview/+/kitkat-mr1-release/chromium/java/com/android/webview/chromium/WebViewContentsClientAdapter.java
         * WebViewContentsClientAdapter.NullWebViewClient.shouldOverrideUrlLoading
         *
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent;
            // Perform generic parsing of the URI to turn it into an Intent.
            try {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException ex) {
                Log.w(K9.LOG_TAG, "Bad URI " + url + ": " + ex.getMessage());
                return false;
            }
            // Sanitize the Intent, ensuring web pages can not bypass browser
            // security (only access to BROWSABLE activities).
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setComponent(null);
            // Pass the package name as application ID so that the intent from
            // the same application can be opened in the same tab.
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, view.getContext()
                    .getPackageName());
            try {
                view.getContext().startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                Log.w(K9.LOG_TAG, "No application can handle " + url);
                return false;
            }
            return true;
        }
    }
}
