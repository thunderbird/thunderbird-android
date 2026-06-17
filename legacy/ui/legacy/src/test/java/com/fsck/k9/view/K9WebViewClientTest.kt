package com.fsck.k9.view

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.fsck.k9.helper.ClipboardManager
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class K9WebViewClientTest : RobolectricTest() {
    private val context = RuntimeEnvironment.getApplication()
    private val testSubject = K9WebViewClient(
        clipboardManager = ClipboardManager(context),
        attachmentResolver = null,
        onPageFinishedListener = null,
    )

    @Test
    fun `shouldInterceptRequest should block insecure HTTP resources`() {
        val request = FakeWebResourceRequest(Uri.parse("http://tracking.example/pixel.png"))

        val result = testSubject.shouldInterceptRequest(WebView(context), request)

        assertThat(result).isNotNull()
    }

    @Test
    fun `shouldInterceptRequest should not block HTTPS resources`() {
        val request = FakeWebResourceRequest(Uri.parse("https://images.example/logo.png"))

        val result = testSubject.shouldInterceptRequest(WebView(context), request)

        assertThat(result).isNull()
    }
}

private class FakeWebResourceRequest(
    private val uri: Uri,
) : WebResourceRequest {
    override fun getUrl(): Uri = uri
    override fun isForMainFrame(): Boolean = false
    override fun isRedirect(): Boolean = false
    override fun hasGesture(): Boolean = false
    override fun getMethod(): String = "GET"
    override fun getRequestHeaders(): Map<String, String> = emptyMap()
}
