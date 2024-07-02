package com.fsck.k9

import com.fsck.k9.autocrypt.autocryptModule
import com.fsck.k9.controller.controllerModule
import com.fsck.k9.controller.push.controllerPushModule
import com.fsck.k9.crypto.openPgpModule
import com.fsck.k9.helper.helperModule
import com.fsck.k9.job.jobModule
import com.fsck.k9.logging.loggingModule
import com.fsck.k9.mailstore.mailStoreModule
import com.fsck.k9.message.extractors.extractorModule
import com.fsck.k9.message.html.htmlModule
import com.fsck.k9.message.quote.quoteModule
import com.fsck.k9.network.connectivityModule
import com.fsck.k9.notification.coreNotificationModule
import com.fsck.k9.power.powerModule
import com.fsck.k9.preferences.preferencesModule

val coreModules = listOf(
    mainModule,
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
    connectivityModule,
    powerModule,
    loggingModule,
)
