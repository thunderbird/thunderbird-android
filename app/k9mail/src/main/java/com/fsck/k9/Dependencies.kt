package com.fsck.k9

import com.fsck.k9.external.BroadcastSenderListener
import com.fsck.k9.external.externalModule
import com.fsck.k9.widget.list.MessageListWidgetUpdateListener
import com.fsck.k9.widget.unread.UnreadWidgetUpdateListener
import org.koin.dsl.module.applicationContext

private val mainAppModule = applicationContext {
    bean { MessagingListenerProvider(
            listOf(
                    get<UnreadWidgetUpdateListener>(),
                    get<MessageListWidgetUpdateListener>(),
                    get<BroadcastSenderListener>()
            ))
    }
}

val appModules = listOf(
        mainAppModule,
        externalModule
)
