package com.fsck.k9

import com.fsck.k9.autocrypt.autocryptModule
import com.fsck.k9.controller.controllerModule
import com.fsck.k9.crypto.openPgpModule
import com.fsck.k9.helper.helperModule
import com.fsck.k9.job.jobModule
import com.fsck.k9.mailstore.mailStoreModule
import com.fsck.k9.message.extractors.extractorModule
import com.fsck.k9.message.html.htmlModule
import com.fsck.k9.message.quote.quoteModule
import com.fsck.k9.notification.coreNotificationModule
import com.fsck.k9.search.searchModule

val coreModules = listOf(
        mainModule,
        openPgpModule,
        autocryptModule,
        mailStoreModule,
        searchModule,
        extractorModule,
        htmlModule,
        quoteModule,
        coreNotificationModule,
        controllerModule,
        jobModule,
        helperModule
)
