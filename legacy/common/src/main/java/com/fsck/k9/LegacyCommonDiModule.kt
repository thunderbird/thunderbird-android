package com.fsck.k9

import app.k9mail.feature.widget.message.list.featureWidgetMessageListDiModule
import app.k9mail.feature.widget.unread.UnreadWidgetUpdateListener
import app.k9mail.feature.widget.unread.featureWidgetUnreadDiModule
import com.fsck.k9.account.newAccountDiModule
import com.fsck.k9.backends.backendsDiModule
import com.fsck.k9.controller.ControllerExtension
import com.fsck.k9.crypto.EncryptionExtractor
import com.fsck.k9.crypto.openpgp.OpenPgpEncryptionExtractor
import com.fsck.k9.notification.notificationDiModule
import com.fsck.k9.preferences.K9StoragePersister
import com.fsck.k9.resources.resourcesDiModule
import com.fsck.k9.storage.legacyStorageDiModule
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.provider.evaluator.MultiFeatureFlagProviderEvaluator
import net.thunderbird.core.preference.storage.StoragePersister
import org.koin.core.qualifier.named
import org.koin.dsl.module

val legacyCommonDiModule = module {
    single<MessagingListenerProvider> {
        DefaultMessagingListenerProvider(
            listeners = listOf(
                get<UnreadWidgetUpdateListener>(),
            ),
        )
    }
    single(named("controllerExtensions")) { emptyList<ControllerExtension>() }
    single<EncryptionExtractor> { OpenPgpEncryptionExtractor.newInstance() }
    single<StoragePersister> {
        K9StoragePersister(get(), get())
    }
    single<FeatureFlagProvider> { get<MultiFeatureFlagProviderEvaluator>() }

    includes(
        featureWidgetMessageListDiModule,
        featureWidgetUnreadDiModule,
        notificationDiModule,
        resourcesDiModule,
        backendsDiModule,
        legacyStorageDiModule,
        newAccountDiModule,
    )
}
