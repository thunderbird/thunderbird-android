package com.fsck.k9.view;


import java.io.InputStream;
import java.util.Stack;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalMessageExtractor;


/**
 * {@link WebViewClient} that intercepts requests for {@code cid:} URIs to load the respective body part.
 */
public abstract class K9WebViewClient extends WebViewClient {
    private static final String CID_SCHEME = "cid";
    private static final WebResourceResponse RESULT_DO_NOT_INTERCEPT = null;
    private static final WebResourceResponse RESULT_DUMMY_RESPONSE = new WebResourceResponse(null, null, null);

    public static WebViewClient newInstance(Part part) {
        if (Build.VERSION.SDK_INT < 21) {
            return new PreLollipopWebViewClient(part);
        }

        return new LollipopWebViewClient(part);
    }


    private final Part part;

    private K9WebViewClient(Part part) {
        this.part = part;
    }

    protected WebResourceResponse shouldInterceptRequest(WebView webView, Uri uri) {
        if (!CID_SCHEME.equals(uri.getScheme())) {
            return RESULT_DO_NOT_INTERCEPT;
        }

        String cid = uri.getSchemeSpecificPart();
        if (TextUtils.isEmpty(cid)) {
            return RESULT_DUMMY_RESPONSE;
        }

        Part part = getPartForContentId(cid);
        if (part == null) {
            return RESULT_DUMMY_RESPONSE;
        }

        Context context = webView.getContext();
        ContentResolver contentResolver = context.getContentResolver();
        try {
            AttachmentViewInfo attachmentInfo = LocalMessageExtractor.extractAttachmentInfo(context, part);
            String mimeType = attachmentInfo.mimeType;
            InputStream inputStream = contentResolver.openInputStream(attachmentInfo.uri);

            return new WebResourceResponse(mimeType, null, inputStream);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Error while intercepting URI: " + uri, e);
            return RESULT_DUMMY_RESPONSE;
        }
    }

    private Part getPartForContentId(String cid) {
        Stack<Part> partsToCheck = new Stack<Part>();
        partsToCheck.push(part);

        while (!partsToCheck.isEmpty()) {
            Part part = partsToCheck.pop();

            Body body = part.getBody();
            if (body instanceof Multipart) {
                Multipart multipart = (Multipart) body;
                for (Part bodyPart : multipart.getBodyParts()) {
                    partsToCheck.push(bodyPart);
                }
            } else if (cid.equals(part.getContentId())) {
                return part;
            }
        }

        return null;
    }


    private static class PreLollipopWebViewClient extends K9WebViewClient {
        protected PreLollipopWebViewClient(Part part) {
            super(part);
        }

        @SuppressWarnings("deprecation")
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView webView, String url) {
            return shouldInterceptRequest(webView, Uri.parse(url));
        }
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    private static class LollipopWebViewClient extends K9WebViewClient {
        protected LollipopWebViewClient(Part part) {
            super(part);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest request) {
            return shouldInterceptRequest(webView, request.getUrl());
        }
    }
}
