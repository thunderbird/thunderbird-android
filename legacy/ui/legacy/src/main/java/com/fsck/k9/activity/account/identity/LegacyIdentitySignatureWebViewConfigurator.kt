package com.fsck.k9.activity.account.identity

import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.fsck.k9.ui.helper.DisplayHtmlUiFactory
import com.fsck.k9.view.MessageWebView
import com.fsck.k9.view.WebViewConfigProvider
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thunderbird.core.android.webkit.WebViewConfig
import net.thunderbird.feature.mail.message.composer.signature.HtmlSignatureSanitizer
import net.thunderbird.feature.mail.message.composer.signature.configureForSignaturePreview

internal class LegacyIdentitySignatureWebViewConfigurator(
    private val webViewConfigProvider: WebViewConfigProvider,
    displayHtmlUiFactory: DisplayHtmlUiFactory,
    private val htmlSignatureSanitizer: HtmlSignatureSanitizer,
) {
    companion object {
        val PREVIEW_DEBOUNCE = 300.milliseconds
    }

    private val displayHtml = displayHtmlUiFactory.createForMessageCompose()
    private val signatureHtml = MutableStateFlow<String?>(value = null)

    fun buildWebConfig(): WebViewConfig = webViewConfigProvider.createForMessageCompose()

    fun buildSignatureHtmlPreviewText(signature: String?): String? {
        val sanitized = htmlSignatureSanitizer.sanitize(html = signature ?: return null)
        return displayHtml.wrapMessageContent(sanitized)
    }

    @OptIn(FlowPreview::class)
    fun configureWebView(
        scope: CoroutineScope,
        lifecycle: Lifecycle,
        webview: MessageWebView,
        signatureEditText: EditText,
        webViewConfig: WebViewConfig = buildWebConfig(),
    ) {
        webview.configureForSignaturePreview(webViewConfig)
        signatureEditText.doAfterTextChanged { signature ->
            println("[signature] after text changed. from ${signatureHtml.value} to $signature")

            signatureHtml.update { signature?.toString() }
        }

        scope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                signatureHtml
                    .debounce(PREVIEW_DEBOUNCE)
                    .collect { signature ->
                        println("[signature] html update to $signature")
                        webview.displayHtmlContentWithInlineAttachments(
                            htmlText = buildSignatureHtmlPreviewText(signature).orEmpty(),
                            attachmentResolver = null,
                            onPageFinishedListener = {},
                        )
                    }
            }
        }
    }
}
