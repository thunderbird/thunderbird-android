package com.fsck.k9.preferences.upgrader

import app.k9mail.legacy.account.Account.FolderMode
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import com.fsck.k9.preferences.InternalSettingsMap
import com.fsck.k9.preferences.ValidatedSettings
import com.fsck.k9.preferences.legacy.FolderClass
import kotlin.test.Test

class CombinedSettingsUpgraderTo98Test {
    private val upgrader = CombinedSettingsUpgraderTo98()

    @Test
    fun `push folders = FolderMode_NONE`() {
        val folderOne = createFolder("One", pushMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", pushMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", pushMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", pushMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderPushMode" to FolderMode.NONE,
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
            folderOne.withPushEnabled(false),
            folderTwo.withPushEnabled(false),
            folderThree.withPushEnabled(false),
            folderFour.withPushEnabled(false),
        )
    }

    @Test
    fun `push folders = FolderMode_ALL`() {
        val folderOne = createFolder("One", pushMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", pushMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", pushMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", pushMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderPushMode" to FolderMode.ALL,
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
            folderOne.withPushEnabled(true),
            folderTwo.withPushEnabled(true),
            folderThree.withPushEnabled(true),
            folderFour.withPushEnabled(true),
        )
    }

    @Test
    fun `push folders = FolderMode_FIRST_CLASS`() {
        val folderOne = createFolder("One", pushMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", pushMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", pushMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", pushMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val folderFive = createFolder("Five", pushMode = FolderClass.INHERITED, displayMode = FolderClass.FIRST_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderPushMode" to FolderMode.FIRST_CLASS,
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
            folderOne.withPushEnabled(true),
            folderTwo.withPushEnabled(false),
            folderThree.withPushEnabled(false),
            folderFour.withPushEnabled(false),
            folderFive.withPushEnabled(true),
        )
    }

    @Test
    fun `push folders = FolderMode_FIRST_AND_SECOND_CLASS`() {
        val folderOne = createFolder("One", pushMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", pushMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", pushMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", pushMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val folderFive = createFolder("Five", pushMode = FolderClass.INHERITED, displayMode = FolderClass.FIRST_CLASS)
        val folderSix = createFolder("Six", pushMode = FolderClass.INHERITED, displayMode = FolderClass.SECOND_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderPushMode" to FolderMode.FIRST_AND_SECOND_CLASS,
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
            folderOne.withPushEnabled(true),
            folderTwo.withPushEnabled(true),
            folderThree.withPushEnabled(false),
            folderFour.withPushEnabled(false),
            folderFive.withPushEnabled(true),
            folderSix.withPushEnabled(true),
        )
    }

    @Test
    fun `push folders = FolderMode_NOT_SECOND_CLASS`() {
        val folderOne = createFolder("One", pushMode = FolderClass.FIRST_CLASS)
        val folderTwo = createFolder("Two", pushMode = FolderClass.SECOND_CLASS)
        val folderThree = createFolder("Three", pushMode = FolderClass.NO_CLASS)
        val folderFour = createFolder("Four", pushMode = FolderClass.INHERITED, displayMode = FolderClass.NO_CLASS)
        val folderFive = createFolder("Five", pushMode = FolderClass.INHERITED, displayMode = FolderClass.FIRST_CLASS)
        val folderSix = createFolder("Six", pushMode = FolderClass.INHERITED, displayMode = FolderClass.SECOND_CLASS)
        val account = createAccount(
            accountSettings = mapOf(
                "folderPushMode" to FolderMode.NOT_SECOND_CLASS,
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
            folderOne.withPushEnabled(true),
            folderTwo.withPushEnabled(false),
            folderThree.withPushEnabled(true),
            folderFour.withPushEnabled(true),
            folderFive.withPushEnabled(true),
            folderSix.withPushEnabled(false),
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
        pushMode: FolderClass = FolderClass.INHERITED,
        displayMode: FolderClass = FolderClass.NO_CLASS,
    ): ValidatedSettings.Folder {
        return ValidatedSettings.Folder(
            name = name,
            settings = mapOf(
                "displayMode" to displayMode,
                "notificationsEnabled" to false,
                "syncMode" to FolderClass.INHERITED,
                "pushMode" to pushMode,
                "inTopGroup" to false,
                "integrate" to false,
            ),
        )
    }

    private fun ValidatedSettings.Folder.withPushEnabled(pushEnabled: Boolean): ValidatedSettings.Folder {
        return this.copy(
            settings = this.settings.toMutableMap().apply {
                this["pushEnabled"] = pushEnabled
            },
        )
    }
}
