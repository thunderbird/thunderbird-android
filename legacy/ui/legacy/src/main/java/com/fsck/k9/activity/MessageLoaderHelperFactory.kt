package com.fsck.k9.activity

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.loader.app.LoaderManager
import com.fsck.k9.activity.MessageLoaderHelper.MessageLoaderCallbacks
import com.fsck.k9.mailstore.MessageViewInfoExtractorFactory
import net.thunderbird.feature.mail.message.composer.html.MessageComposerHtmlSettingsProvider
import net.thunderbird.feature.mail.message.reader.api.html.MessageReaderHtmlSettingsProvider

class MessageLoaderHelperFactory(
    private val messageViewInfoExtractorFactory: MessageViewInfoExtractorFactory,
    private val messageReaderHtmlSettingsProvider: MessageReaderHtmlSettingsProvider,
    private val messageComposerHtmlSettingsProvider: MessageComposerHtmlSettingsProvider,
) {
    fun createForMessageView(
        context: Context,
        loaderManager: LoaderManager,
        fragmentManager: FragmentManager,
        callback: MessageLoaderCallbacks,
    ): MessageLoaderHelper {
        val htmlSettings = messageReaderHtmlSettingsProvider.create()
        val messageViewInfoExtractor = messageViewInfoExtractorFactory.create(htmlSettings)
        return MessageLoaderHelper(context, loaderManager, fragmentManager, callback, messageViewInfoExtractor)
    }

    fun createForMessageCompose(
        context: Context,
        loaderManager: LoaderManager,
        fragmentManager: FragmentManager,
        callback: MessageLoaderCallbacks,
    ): MessageLoaderHelper {
        val htmlSettings = messageComposerHtmlSettingsProvider.create()
        val messageViewInfoExtractor = messageViewInfoExtractorFactory.create(htmlSettings)
        return MessageLoaderHelper(context, loaderManager, fragmentManager, callback, messageViewInfoExtractor)
    }
}
