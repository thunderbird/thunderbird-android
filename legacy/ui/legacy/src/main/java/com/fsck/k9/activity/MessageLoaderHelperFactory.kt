package com.fsck.k9.activity

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.loader.app.LoaderManager
import com.fsck.k9.activity.MessageLoaderHelper.MessageLoaderCallbacks
import com.fsck.k9.mailstore.MessageViewInfoExtractorFactory
import com.fsck.k9.ui.helper.HtmlSettingsProvider

class MessageLoaderHelperFactory(
    private val messageViewInfoExtractorFactory: MessageViewInfoExtractorFactory,
    private val htmlSettingsProvider: HtmlSettingsProvider,
) {
    fun createForMessageView(
        context: Context,
        loaderManager: LoaderManager,
        fragmentManager: FragmentManager,
        callback: MessageLoaderCallbacks,
    ): MessageLoaderHelper {
        val htmlSettings = htmlSettingsProvider.createForMessageView()
        val messageViewInfoExtractor = messageViewInfoExtractorFactory.create(htmlSettings)
        return MessageLoaderHelper(context, loaderManager, fragmentManager, callback, messageViewInfoExtractor)
    }

    fun createForMessageCompose(
        context: Context,
        loaderManager: LoaderManager,
        fragmentManager: FragmentManager,
        callback: MessageLoaderCallbacks,
    ): MessageLoaderHelper {
        val htmlSettings = htmlSettingsProvider.createForMessageCompose()
        val messageViewInfoExtractor = messageViewInfoExtractorFactory.create(htmlSettings)
        return MessageLoaderHelper(context, loaderManager, fragmentManager, callback, messageViewInfoExtractor)
    }
}
