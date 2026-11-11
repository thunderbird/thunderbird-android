package com.fsck.k9

import app.k9mail.feature.account.oauth.featureAccountOAuthDiModule
import app.k9mail.feature.launcher.featureLauncherDiModule
import app.k9mail.legacy.ui.folder.legacyUiFolderDiModule
import com.fsck.k9.activity.activityDiModule
import com.fsck.k9.activity.setup.setUpDiModule
import com.fsck.k9.contacts.contactsDiModule
import com.fsck.k9.ui.account.accountUiDiModule
import com.fsck.k9.ui.base.legacyUiBaseDiModule
import com.fsck.k9.ui.choosefolder.chooseFolderUiDiModule
import com.fsck.k9.ui.compose.composeDiModule
import com.fsck.k9.ui.endtoend.endToEndUiDiModule
import com.fsck.k9.ui.folders.foldersUiDiModule
import com.fsck.k9.ui.identity.identityUiDiModule
import com.fsck.k9.ui.managefolders.manageFoldersUiDiModule
import com.fsck.k9.ui.messagedetails.messageDetailsUiDiModule
import com.fsck.k9.ui.messagelist.messageListUiDiModule
import com.fsck.k9.ui.messagesource.messageSourceDiModule
import com.fsck.k9.ui.messageview.messageViewUiDiModule
import com.fsck.k9.ui.settings.settingsUiDiModule
import com.fsck.k9.ui.uiDiModule
import com.fsck.k9.view.viewDiModule
import net.thunderbird.feature.changelog.internal.featureChangelogDiModule
import org.koin.dsl.module

val legacyUiDiModule = module {
    includes(
        featureAccountOAuthDiModule,
        legacyUiBaseDiModule,
        activityDiModule,
        uiDiModule,
        legacyUiFolderDiModule,
        settingsUiDiModule,
        endToEndUiDiModule,
        foldersUiDiModule,
        messageListUiDiModule,
        manageFoldersUiDiModule,
        chooseFolderUiDiModule,
        contactsDiModule,
        composeDiModule,
        viewDiModule,
        featureChangelogDiModule,
        messageSourceDiModule,
        accountUiDiModule,
        messageDetailsUiDiModule,
        messageViewUiDiModule,
        identityUiDiModule,
        featureLauncherDiModule,
        setUpDiModule,
    )
}
