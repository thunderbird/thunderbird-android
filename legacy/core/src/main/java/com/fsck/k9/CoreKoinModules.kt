package com.fsck.k9

import com.fsck.k9.autocrypt.autocryptModule
import com.fsck.k9.controller.controllerModule
import com.fsck.k9.controller.push.controllerPushModule
import com.fsck.k9.crypto.openPgpModule
import com.fsck.k9.helper.helperModule
import com.fsck.k9.job.jobModule
import com.fsck.k9.mailstore.mailStoreModule
import com.fsck.k9.message.extractors.extractorModule
import com.fsck.k9.message.html.htmlModule
import com.fsck.k9.message.quote.quoteModule
import com.fsck.k9.notification.coreNotificationModule
import com.fsck.k9.power.powerModule
import com.fsck.k9.preferences.preferencesModule
import net.thunderbird.core.android.logging.loggingModule
import net.thunderbird.core.android.network.coreAndroidNetworkModule
import net.thunderbird.feature.account.storage.legacy.featureAccountStorageLegacyModule

val legacyCoreModules = listOf(
    mainModule,
    coreAndroidNetworkModule,
    openPgpModule,
    autocryptModule,
    mailStoreModule,
    extractorModule,
    htmlModule,
    quoteModule,
    coreNotificationModule,
    controllerModule,
    controllerPushModule,
    jobModule,
    helperModule,
    preferencesModule,
    powerModule,
    loggingModule,
    featureAccountStorageLegacyModule,
)
