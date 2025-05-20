package com.fsck.k9.preferences.upgrader

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import com.fsck.k9.preferences.InternalSettingsMap
import com.fsck.k9.preferences.ValidatedSettings
import com.fsck.k9.preferences.legacy.FolderClass
import kotlin.test.Test
import net.thunderbird.core.android.account.FolderMode

class CombinedSettingsUpgraderTo99Test {
    private val upgrader = CombinedSettingsUpgraderTo99()

    @Test
    fun `sync folders = FolderMode_NONE`() {
        val folderOne = createFolder("One", syncMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", syncMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", syncMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", syncMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderSyncMode" to FolderMode.NONE,
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
            folderOne.withSyncEnabled(false),
            folderTwo.withSyncEnabled(false),
            folderThree.withSyncEnabled(false),
            folderFour.withSyncEnabled(false),
        )
    }

    @Test
    fun `sync folders = FolderMode_ALL`() {
        val folderOne = createFolder("One", syncMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", syncMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", syncMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", syncMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderSyncMode" to FolderMode.ALL,
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
            folderOne.withSyncEnabled(true),
            folderTwo.withSyncEnabled(true),
            folderThree.withSyncEnabled(true),
            folderFour.withSyncEnabled(true),
        )
    }

    @Test
    fun `sync folders = FolderMode_FIRST_CLASS`() {
        val folderOne = createFolder("One", syncMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", syncMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", syncMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", syncMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val folderFive = createFolder("Five", syncMode = FolderClass.INHERITED, displayMode = FolderClass.FIRST_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderSyncMode" to FolderMode.FIRST_CLASS,
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
            folderOne.withSyncEnabled(true),
            folderTwo.withSyncEnabled(false),
            folderThree.withSyncEnabled(false),
            folderFour.withSyncEnabled(false),
            folderFive.withSyncEnabled(true),
        )
    }

    @Test
    fun `sync folders = FolderMode_FIRST_AND_SECOND_CLASS`() {
        val folderOne = createFolder("One", syncMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", syncMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", syncMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", syncMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val folderFive = createFolder("Five", syncMode = FolderClass.INHERITED, displayMode = FolderClass.FIRST_CLASS)
        val folderSix = createFolder("Six", syncMode = FolderClass.INHERITED, displayMode = FolderClass.SECOND_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderSyncMode" to FolderMode.FIRST_AND_SECOND_CLASS,
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
            folderOne.withSyncEnabled(true),
            folderTwo.withSyncEnabled(true),
            folderThree.withSyncEnabled(false),
            folderFour.withSyncEnabled(false),
            folderFive.withSyncEnabled(true),
            folderSix.withSyncEnabled(true),
        )
    }

    @Test
    fun `sync folders = FolderMode_NOT_SECOND_CLASS`() {
        val folderOne = createFolder("One", syncMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", syncMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", syncMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", syncMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val folderFive = createFolder("Five", syncMode = FolderClass.INHERITED, displayMode = FolderClass.FIRST_CLASS)
        val folderSix = createFolder("Six", syncMode = FolderClass.INHERITED, displayMode = FolderClass.SECOND_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderSyncMode" to FolderMode.NOT_SECOND_CLASS,
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
            folderOne.withSyncEnabled(true),
            folderTwo.withSyncEnabled(false),
            folderThree.withSyncEnabled(true),
            folderFour.withSyncEnabled(true),
            folderFive.withSyncEnabled(true),
            folderSix.withSyncEnabled(false),
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
        syncMode: FolderClass = FolderClass.INHERITED,
        displayMode: FolderClass = FolderClass.NO_CLASS,
    ): ValidatedSettings.Folder {
        return ValidatedSettings.Folder(
            name = name,
            settings = mapOf(
                "displayMode" to displayMode,
                "notificationsEnabled" to false,
                "syncMode" to syncMode,
                "pushEnabled" to false,
                "inTopGroup" to false,
                "integrate" to false,
            ),
        )
    }

    private fun ValidatedSettings.Folder.withSyncEnabled(syncEnabled: Boolean): ValidatedSettings.Folder {
        return this.copy(
            settings = this.settings.toMutableMap().apply {
                this["syncEnabled"] = syncEnabled
            },
        )
    }
}
