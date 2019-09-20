package com.fsck.k9

import com.fsck.k9.account.accountModule
import com.fsck.k9.activity.activityModule
import com.fsck.k9.autodiscovery.autodiscoveryModule
import com.fsck.k9.contacts.contactsModule
import com.fsck.k9.fragment.fragmentModule
import com.fsck.k9.ui.endtoend.endToEndUiModule
import com.fsck.k9.ui.settings.settingsUiModule
import com.fsck.k9.ui.uiModule
import com.fsck.k9.view.viewModule

val uiModules = listOf(
        activityModule,
        uiModule,
        settingsUiModule,
        endToEndUiModule,
        fragmentModule,
        contactsModule,
        accountModule,
        autodiscoveryModule,
        viewModule
)
