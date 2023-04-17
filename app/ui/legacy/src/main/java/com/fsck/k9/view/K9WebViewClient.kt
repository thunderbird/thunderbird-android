package com.fsck.k9.view

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Browser
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.fsck.k9.logging.Timber
import com.fsck.k9.mailstore.AttachmentResolver
import com.fsck.k9.ui.R
import com.fsck.k9.view.MessageWebView.OnPageFinishedListener

/**
 * [WebViewClient] that intercepts requests for `cid:` URIs to load the respective body part.
 */
internal class K9WebViewClient(
    private val attachmentResolver: AttachmentResolver?,
) : WebViewClient() {
    private var onPageFinishedListener: OnPageFinishedListener? = null

    @Deprecated("Deprecated in parent class")
    override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean {
        return shouldOverrideUrlLoading(webView, Uri.parse(url))
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(webView: WebView, request: WebResourceRequest): Boolean {
        return shouldOverrideUrlLoading(webView, request.url)
    }

    private fun shouldOverrideUrlLoading(webView: WebView, uri: Uri): Boolean {
        if (uri.scheme == CID_SCHEME) return false

        val context = webView.context
        val intent = createBrowserViewIntent(uri, context)

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.d(e, "Couldn't open URL: %s", uri)
            Toast.makeText(context, R.string.error_activity_not_found, Toast.LENGTH_LONG).show()
        }

        return true
    }

    private fun createBrowserViewIntent(uri: Uri, context: Context): Intent {
        return Intent(Intent.ACTION_VIEW, uri).apply {
            putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
            putExtra(Browser.EXTRA_CREATE_NEW_TAB, true)

            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
    }

    override fun shouldInterceptRequest(webView: WebView, request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url

        return if (uri.scheme == CID_SCHEME) {
            handleCidUri(uri, webView)
        } else {
            RESULT_DO_NOT_INTERCEPT
        }
    }

    private fun handleCidUri(uri: Uri, webView: WebView): WebResourceResponse {
        val attachmentUri = getAttachmentUriFromCidUri(uri) ?: return RESULT_DUMMY_RESPONSE

        val context = webView.context
        val contentResolver = context.contentResolver

        @Suppress("TooGenericExceptionCaught")
        return try {
            val mimeType = contentResolver.getType(attachmentUri)
            val inputStream = contentResolver.openInputStream(attachmentUri)

            WebResourceResponse(mimeType, null, inputStream).apply {
                addCacheControlHeader()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while intercepting URI: %s", uri)
            RESULT_DUMMY_RESPONSE
        }
    }

    private fun getAttachmentUriFromCidUri(uri: Uri): Uri? {
        return uri.schemeSpecificPart
            ?.let { cid -> attachmentResolver?.getAttachmentUriForContentId(cid) }
    }

    private fun WebResourceResponse.addCacheControlHeader() {
        responseHeaders = mapOf("Cache-Control" to "no-store")
    }

    fun setOnPageFinishedListener(onPageFinishedListener: OnPageFinishedListener?) {
        this.onPageFinishedListener = onPageFinishedListener
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)

        onPageFinishedListener?.onPageFinished()
    }

    companion object {
        private const val CID_SCHEME = "cid"

        private val RESULT_DO_NOT_INTERCEPT: WebResourceResponse? = null
        private val RESULT_DUMMY_RESPONSE = WebResourceResponse(null, null, null)

        fun newInstance(attachmentResolver: AttachmentResolver?): K9WebViewClient {
            return K9WebViewClient(attachmentResolver)
        }
    }
}
