package com.fsck.k9.preferences.upgrader

import app.k9mail.legacy.account.Account.FolderMode
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import com.fsck.k9.preferences.InternalSettingsMap
import com.fsck.k9.preferences.ValidatedSettings
import com.fsck.k9.preferences.legacy.FolderClass
import kotlin.test.Test

class CombinedSettingsUpgraderTo96Test {
    private val upgrader = CombinedSettingsUpgraderTo96()

    @Test
    fun `notification folders = FolderMode_NONE`() {
        val folderOne = createFolder("One", notifyMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", notifyMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", notifyMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", notifyMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderNotifyNewMailMode" to FolderMode.NONE,
            ),
            folders = listOf(
                folderOne,
                folderTwo,
                folderThree,
                folderFour,
            ),
        )

        val result = upgrader.upgrade(account)

        assertThat(result.folders).containsExactlyInAnyOrder(
            folderOne.withNotificationsEnabled(false),
            folderTwo.withNotificationsEnabled(false),
            folderThree.withNotificationsEnabled(false),
            folderFour.withNotificationsEnabled(false),
        )
    }

    @Test
    fun `notification folders = FolderMode_ALL`() {
        val folderOne = createFolder("One", notifyMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", notifyMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", notifyMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", notifyMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderNotifyNewMailMode" to FolderMode.ALL,
            ),
            folders = listOf(
                folderOne,
                folderTwo,
                folderThree,
                folderFour,
            ),
        )

        val result = upgrader.upgrade(account)

        assertThat(result.folders).containsExactlyInAnyOrder(
            folderOne.withNotificationsEnabled(true),
            folderTwo.withNotificationsEnabled(true),
            folderThree.withNotificationsEnabled(true),
            folderFour.withNotificationsEnabled(true),
        )
    }

    @Test
    fun `notification folders = FolderMode_FIRST_CLASS`() {
        val folderOne = createFolder("One", notifyMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", notifyMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", notifyMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", notifyMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val folderFive = createFolder("Five", notifyMode = FolderClass.INHERITED, displayMode = FolderClass.FIRST_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderNotifyNewMailMode" to FolderMode.FIRST_CLASS,
            ),
            folders = listOf(
                folderOne,
                folderTwo,
                folderThree,
                folderFour,
                folderFive,
            ),
        )

        val result = upgrader.upgrade(account)

        assertThat(result.folders).containsExactlyInAnyOrder(
            folderOne.withNotificationsEnabled(true),
            folderTwo.withNotificationsEnabled(false),
            folderThree.withNotificationsEnabled(false),
            folderFour.withNotificationsEnabled(false),
            folderFive.withNotificationsEnabled(true),
        )
    }

    @Test
    fun `notification folders = FolderMode_FIRST_AND_SECOND_CLASS`() {
        val folderOne = createFolder("One", notifyMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", notifyMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", notifyMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", notifyMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val folderFive = createFolder("Five", notifyMode = FolderClass.INHERITED, displayMode = FolderClass.FIRST_CLASS)
        val folderSix = createFolder("Six", notifyMode = FolderClass.INHERITED, displayMode = FolderClass.SECOND_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderNotifyNewMailMode" to FolderMode.FIRST_AND_SECOND_CLASS,
            ),
            folders = listOf(
                folderOne,
                folderTwo,
                folderThree,
                folderFour,
                folderFive,
                folderSix,
            ),
        )

        val result = upgrader.upgrade(account)

        assertThat(result.folders).containsExactlyInAnyOrder(
            folderOne.withNotificationsEnabled(true),
            folderTwo.withNotificationsEnabled(true),
            folderThree.withNotificationsEnabled(false),
            folderFour.withNotificationsEnabled(false),
            folderFive.withNotificationsEnabled(true),
            folderSix.withNotificationsEnabled(true),
        )
    }

    @Test
    fun `notification folders = FolderMode_NOT_SECOND_CLASS`() {
        val folderOne = createFolder("One", notifyMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", notifyMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", notifyMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", notifyMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val folderFive = createFolder("Five", notifyMode = FolderClass.INHERITED, displayMode = FolderClass.FIRST_CLASS)
        val folderSix = createFolder("Six", notifyMode = FolderClass.INHERITED, displayMode = FolderClass.SECOND_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderNotifyNewMailMode" to FolderMode.NOT_SECOND_CLASS,
            ),
            folders = listOf(
                folderOne,
                folderTwo,
                folderThree,
                folderFour,
                folderFive,
                folderSix,
            ),
        )

        val result = upgrader.upgrade(account)

        assertThat(result.folders).containsExactlyInAnyOrder(
            folderOne.withNotificationsEnabled(true),
            folderTwo.withNotificationsEnabled(false),
            folderThree.withNotificationsEnabled(true),
            folderFour.withNotificationsEnabled(true),
            folderFive.withNotificationsEnabled(true),
            folderSix.withNotificationsEnabled(false),
        )
    }

    @Test
    fun `notifications for special folders should be disabled`() {
        val trashFolder = createFolder("Trash", notifyMode = FolderClass.FIRST_CLASS)
        val draftsFolder = createFolder("Drafts", notifyMode = FolderClass.FIRST_CLASS)
        val spamFolder = createFolder("Spam", notifyMode = FolderClass.FIRST_CLASS)
        val sentFolder = createFolder("Sent", notifyMode = FolderClass.FIRST_CLASS)
        val otherFolder = createFolder("Other", notifyMode = FolderClass.FIRST_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderNotifyNewMailMode" to FolderMode.ALL,
                "trashFolderName" to "Trash",
                "draftsFolderName" to "Drafts",
                "spamFolderName" to "Spam",
                "sentFolderName" to "Sent",
            ),
            folders = listOf(
                trashFolder,
                draftsFolder,
                spamFolder,
                sentFolder,
                otherFolder,
            ),
        )

        val result = upgrader.upgrade(account)

        assertThat(result.folders).containsExactlyInAnyOrder(
            trashFolder.withNotificationsEnabled(false),
            draftsFolder.withNotificationsEnabled(false),
            spamFolder.withNotificationsEnabled(false),
            sentFolder.withNotificationsEnabled(false),
            otherFolder.withNotificationsEnabled(true),
        )
    }

    @Test
    fun `notifications for special folders that point to the inbox should follow inbox notification class`() {
        val inboxFolder = createFolder("INBOX", notifyMode = FolderClass.FIRST_CLASS)
        val trashFolder = createFolder("Trash", notifyMode = FolderClass.FIRST_CLASS)
        val spamFolder = createFolder("Spam", notifyMode = FolderClass.FIRST_CLASS)
        val otherFolder = createFolder("Other", notifyMode = FolderClass.NO_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderNotifyNewMailMode" to FolderMode.FIRST_CLASS,
                "trashFolderName" to "Trash",
                "draftsFolderName" to "INBOX",
                "spamFolderName" to "Spam",
                "sentFolderName" to "INBOX",
            ),
            folders = listOf(
                inboxFolder,
                trashFolder,
                spamFolder,
                otherFolder,
            ),
        )

        val result = upgrader.upgrade(account)

        assertThat(result.folders).containsExactlyInAnyOrder(
            inboxFolder.withNotificationsEnabled(true),
            trashFolder.withNotificationsEnabled(false),
            spamFolder.withNotificationsEnabled(false),
            otherFolder.withNotificationsEnabled(false),
        )
    }

    private fun createAccount(
        accountSettings: InternalSettingsMap,
        folders: List<ValidatedSettings.Folder>,
    ): ValidatedSettings.Account {
        return ValidatedSettings.Account(
            uuid = "irrelevant",
            name = "irrelevant",
            incoming = irrelevantServer(),
            outgoing = irrelevantServer(),
            settings = accountSettings,
            identities = emptyList(),
            folders = folders,
        )
    }

    private fun irrelevantServer(): ValidatedSettings.Server {
        return ValidatedSettings.Server(
            type = "irrelevant",
            settings = emptyMap(),
            extras = emptyMap(),
        )
    }

    private fun createFolder(
        name: String,
        notifyMode: FolderClass = FolderClass.INHERITED,
        displayMode: FolderClass = FolderClass.NO_CLASS,
    ): ValidatedSettings.Folder {
        return ValidatedSettings.Folder(
            name = name,
            settings = mapOf(
                "displayMode" to displayMode,
                "notifyMode" to notifyMode,
                "syncMode" to FolderClass.INHERITED,
                "pushMode" to FolderClass.INHERITED,
                "inTopGroup" to false,
                "integrate" to false,
            ),
        )
    }

    private fun ValidatedSettings.Folder.withNotificationsEnabled(
        notificationsEnabled: Boolean,
    ): ValidatedSettings.Folder {
        return this.copy(
            settings = this.settings.toMutableMap().apply {
                this["notificationsEnabled"] = notificationsEnabled
            },
        )
    }
}
