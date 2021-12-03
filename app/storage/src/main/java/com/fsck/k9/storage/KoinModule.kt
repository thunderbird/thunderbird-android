package com.fsck.k9.storage

import com.fsck.k9.mailstore.MessageStoreFactory
import com.fsck.k9.mailstore.SchemaDefinitionFactory
import com.fsck.k9.notification.NotificationStoreProvider
import com.fsck.k9.storage.messages.K9MessageStoreFactory
import com.fsck.k9.storage.notifications.K9NotificationStoreProvider
import org.koin.dsl.module

val storageModule = module {
    single<SchemaDefinitionFactory> { K9SchemaDefinitionFactory() }
    single<MessageStoreFactory> {
        K9MessageStoreFactory(localStoreProvider = get(), storageManager = get(), basicPartInfoExtractor = get())
    }
    single<NotificationStoreProvider> {
        K9NotificationStoreProvider(localStoreProvider = get())
    }
}
