package com.fsck.k9.preferences.upgrader

import app.k9mail.legacy.account.Account.FolderMode
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import com.fsck.k9.preferences.InternalSettingsMap
import com.fsck.k9.preferences.ValidatedSettings
import com.fsck.k9.preferences.legacy.FolderClass
import kotlin.test.Test

class CombinedSettingsUpgraderTo100Test {
    private val upgrader = CombinedSettingsUpgraderTo100()

    @Test
    fun `display folders = FolderMode_NONE`() {
        val folderOne = createFolder("One", displayMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", displayMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", displayMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", displayMode = FolderClass.INHERITED)
        val account = createAccount(
            accountSettings = mapOf(
                "folderDisplayMode" to FolderMode.NONE,
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
            folderOne.withVisible(false),
            folderTwo.withVisible(false),
            folderThree.withVisible(false),
            folderFour.withVisible(false),
        )
    }

    @Test
    fun `display folders = FolderMode_ALL`() {
        val folderOne = createFolder("One", displayMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", displayMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", displayMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", displayMode = FolderClass.INHERITED)
        val account = createAccount(
            accountSettings = mapOf(
                "folderDisplayMode" to FolderMode.ALL,
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
            folderOne.withVisible(true),
            folderTwo.withVisible(true),
            folderThree.withVisible(true),
            folderFour.withVisible(true),
        )
    }

    @Test
    fun `display folders = FolderMode_FIRST_CLASS`() {
        val folderOne = createFolder("One", displayMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", displayMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", displayMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", displayMode = FolderClass.INHERITED)
        val account = createAccount(
            accountSettings = mapOf(
                "folderDisplayMode" to FolderMode.FIRST_CLASS,
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
            folderOne.withVisible(true),
            folderTwo.withVisible(false),
            folderThree.withVisible(false),
            folderFour.withVisible(false),
        )
    }

    @Test
    fun `display folders = FolderMode_FIRST_AND_SECOND_CLASS`() {
        val folderOne = createFolder("One", displayMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", displayMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", displayMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", displayMode = FolderClass.INHERITED)
        val account = createAccount(
            accountSettings = mapOf(
                "folderDisplayMode" to FolderMode.FIRST_AND_SECOND_CLASS,
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
            folderOne.withVisible(true),
            folderTwo.withVisible(true),
            folderThree.withVisible(false),
            folderFour.withVisible(false),
        )
    }

    @Test
    fun `display folders = FolderMode_NOT_SECOND_CLASS`() {
        val folderOne = createFolder("One", displayMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", displayMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", displayMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", displayMode = FolderClass.INHERITED)
        val account = createAccount(
            accountSettings = mapOf(
                "folderDisplayMode" to FolderMode.NOT_SECOND_CLASS,
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
            folderOne.withVisible(true),
            folderTwo.withVisible(false),
            folderThree.withVisible(true),
            folderFour.withVisible(true),
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
        displayMode: FolderClass = FolderClass.NO_CLASS,
    ): ValidatedSettings.Folder {
        return ValidatedSettings.Folder(
            name = name,
            settings = mapOf(
                "displayMode" to displayMode,
                "notificationsEnabled" to false,
                "syncEnabled" to false,
                "pushEnabled" to false,
                "inTopGroup" to false,
                "integrate" to false,
            ),
        )
    }

    private fun ValidatedSettings.Folder.withVisible(visible: Boolean): ValidatedSettings.Folder {
        return this.copy(
            settings = this.settings.toMutableMap().apply {
                this["visible"] = visible
            },
        )
    }
}
