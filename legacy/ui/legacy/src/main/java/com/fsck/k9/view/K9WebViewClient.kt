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
import com.fsck.k9.helper.ClipboardManager
import com.fsck.k9.mailstore.AttachmentResolver
import com.fsck.k9.ui.R
import com.fsck.k9.view.MessageWebView.OnPageFinishedListener
import net.thunderbird.core.logging.legacy.Log

/**
 * [WebViewClient] that intercepts requests for `cid:` URIs to load the respective body part.
 */
class K9WebViewClient(
    private val clipboardManager: ClipboardManager,
    private val attachmentResolver: AttachmentResolver?,
    private val onPageFinishedListener: OnPageFinishedListener?,
) : WebViewClient() {

    @Deprecated("Deprecated in parent class")
    override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean {
        return shouldOverrideUrlLoading(webView, Uri.parse(url))
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(webView: WebView, request: WebResourceRequest): Boolean {
        return shouldOverrideUrlLoading(webView, request.url)
    }

    private fun shouldOverrideUrlLoading(webView: WebView, uri: Uri): Boolean {
        return when (uri.scheme) {
            CID_SCHEME -> {
                false
            }
            FILE_SCHEME -> {
                copyUrlToClipboard(webView.context, uri)
                true
            }
            else -> {
                openUrl(webView.context, uri)
                true
            }
        }
    }

    private fun copyUrlToClipboard(context: Context, uri: Uri) {
        val label = context.getString(R.string.webview_contextmenu_link_clipboard_label)
        clipboardManager.setText(label, uri.toString())
    }

    private fun openUrl(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
            putExtra(Browser.EXTRA_CREATE_NEW_TAB, true)

            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.d(e, "Couldn't open URL: %s", uri)
            Toast.makeText(context, R.string.error_activity_not_found, Toast.LENGTH_LONG).show()
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
            Log.e(e, "Error while intercepting URI: %s", uri)
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

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)

        onPageFinishedListener?.onPageFinished()
    }

    companion object {
        private const val CID_SCHEME = "cid"
        private const val FILE_SCHEME = "file"

        private val RESULT_DO_NOT_INTERCEPT: WebResourceResponse? = null
        private val RESULT_DUMMY_RESPONSE = WebResourceResponse(null, null, null)
    }
}
