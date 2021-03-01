package com.fsck.k9

import com.fsck.k9.account.accountModule
import com.fsck.k9.activity.activityModule
import com.fsck.k9.autodiscovery.providersxml.autodiscoveryProvidersXmlModule
import com.fsck.k9.contacts.contactsModule
import com.fsck.k9.fragment.fragmentModule
import com.fsck.k9.ui.account.accountUiModule
import com.fsck.k9.ui.base.uiBaseModule
import com.fsck.k9.ui.changelog.changelogUiModule
import com.fsck.k9.ui.choosefolder.chooseFolderUiModule
import com.fsck.k9.ui.endtoend.endToEndUiModule
import com.fsck.k9.ui.folders.foldersUiModule
import com.fsck.k9.ui.managefolders.manageFoldersUiModule
import com.fsck.k9.ui.messagelist.messageListUiModule
import com.fsck.k9.ui.messagesource.messageSourceModule
import com.fsck.k9.ui.settings.settingsUiModule
import com.fsck.k9.ui.uiModule
import com.fsck.k9.view.viewModule

val uiModules = listOf(
    uiBaseModule,
    activityModule,
    uiModule,
    settingsUiModule,
    endToEndUiModule,
    foldersUiModule,
    messageListUiModule,
    manageFoldersUiModule,
    chooseFolderUiModule,
    fragmentModule,
    contactsModule,
    accountModule,
    autodiscoveryProvidersXmlModule,
    viewModule,
    changelogUiModule,
    messageSourceModule,
    accountUiModule
)
