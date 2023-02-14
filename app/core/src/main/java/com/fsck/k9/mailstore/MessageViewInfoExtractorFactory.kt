package com.fsck.k9.mailstore

import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.message.extractors.AttachmentInfoExtractor
import com.fsck.k9.message.html.HtmlProcessorFactory
import com.fsck.k9.message.html.HtmlSettings

class MessageViewInfoExtractorFactory(
    private val attachmentInfoExtractor: AttachmentInfoExtractor,
    private val htmlProcessorFactory: HtmlProcessorFactory,
    private val resourceProvider: CoreResourceProvider,
) {
    fun create(settings: HtmlSettings): MessageViewInfoExtractor {
        val htmlProcessor = htmlProcessorFactory.create(settings)
        return MessageViewInfoExtractor(attachmentInfoExtractor, htmlProcessor, resourceProvider)
    }
}
