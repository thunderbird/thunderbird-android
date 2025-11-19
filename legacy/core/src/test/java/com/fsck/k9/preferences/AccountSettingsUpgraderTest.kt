package com.fsck.k9.preferences

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import com.fsck.k9.preferences.Settings.BooleanSetting
import com.fsck.k9.preferences.Settings.StringSetting
import kotlin.test.Test
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.GeneralSettings
import org.junit.Before
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class AccountSettingsUpgraderTest {

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Suppress("LongMethod")
    @Test
    fun `with combined settings upgrader`() {
        val accountSettingsDescriptions = mapOf(
            "notificationFolderMode" to versions(
                1 to StringSetting("FIRST_CLASS"),
                2 to null,
            ),
            "additional" to versions(
                3 to StringSetting("new"),
            ),
        )
        val folderSettingsUpgrader = FolderSettingsUpgrader(
            settingsDescriptions = mapOf(
                "notificationClass" to versions(
                    1 to StringSetting("NO_CLASS"),
                    2 to null,
                ),
                "notificationEnabled" to versions(
                    2 to BooleanSetting(false),
                ),
            ),
            upgraders = emptyMap(),
            generalSettingsManager = mock {
                on { getConfig() } doReturn
                    GeneralSettings(
                        platformConfigProvider = FakePlatformConfigProvider(),
                    )
            },
        )
        val combinedUpgraders = mapOf(
            2 to {
                CombinedSettingsUpgrader { account ->
                    val notificationFolderMode = account.settings["notificationFolderMode"]
                    account.copy(
                        folders = account.folders.map { folder ->
                            folder.copy(
                                settings = folder.settings.toMutableMap().apply {
                                    this["notificationEnabled"] = this["notificationClass"] == notificationFolderMode
                                },
                            )
                        },
                    )
                }
            },
        )
        val accountSettingsUpgrader = AccountSettingsUpgrader(
            identitySettingsUpgrader = irrelevantIdentitySettingsUpgrader(),
            folderSettingsUpgrader = folderSettingsUpgrader,
            serverSettingsUpgrader = irrelevantServerSettingsUpgrader(),
            latestVersion = 3,
            settingsDescriptions = accountSettingsDescriptions,
            upgraders = emptyMap(),
            combinedUpgraders = combinedUpgraders,
            generalSettingsManager = mock {
                on { getConfig() } doReturn
                    GeneralSettings(
                        platformConfigProvider = FakePlatformConfigProvider(),
                    )
            },
        )
        val account = ValidatedSettings.Account(
            uuid = "irrelevant",
            name = "irrelevant",
            incoming = irrelevantServer(),
            outgoing = irrelevantServer(),
            settings = mapOf(
                "notificationFolderMode" to "SECOND_CLASS",
            ),
            identities = emptyList(),
            folders = listOf(
                ValidatedSettings.Folder(
                    name = "folderOne",
                    settings = mapOf(
                        "notificationClass" to "FIRST_CLASS",
                    ),
                ),
                ValidatedSettings.Folder(
                    name = "folderTwo",
                    settings = mapOf(
                        "notificationClass" to "SECOND_CLASS",
                    ),
                ),
            ),
        )

        val result = accountSettingsUpgrader.upgrade(contentVersion = 1, account)

        assertThat(result).all {
            prop(ValidatedSettings.Account::settings).isEqualTo(mapOf("additional" to "new"))
            prop(ValidatedSettings.Account::folders).containsExactlyInAnyOrder(
                ValidatedSettings.Folder(
                    name = "folderOne",
                    settings = mapOf(
                        "notificationEnabled" to false,
                    ),
                ),
                ValidatedSettings.Folder(
                    name = "folderTwo",
                    settings = mapOf(
                        "notificationEnabled" to true,
                    ),
                ),
            )
        }
    }

    private fun irrelevantIdentitySettingsUpgrader(): IdentitySettingsUpgrader {
        return IdentitySettingsUpgrader(
            settingsDescriptions = emptyMap(),
            upgraders = emptyMap(),
            generalSettingsManager = mock {
                on { getConfig() } doReturn
                    GeneralSettings(
                        platformConfigProvider = FakePlatformConfigProvider(),
                    )
            },
        )
    }

    private fun irrelevantServerSettingsUpgrader(): ServerSettingsUpgrader {
        return ServerSettingsUpgrader(
            settingsDescriptions = emptyMap(),
            upgraders = emptyMap(),
            generalSettingsManager = mock {
                on { getConfig() } doReturn
                    GeneralSettings(
                        platformConfigProvider = FakePlatformConfigProvider(),
                    )
            },
        )
    }

    private fun irrelevantServer(): ValidatedSettings.Server {
        return ValidatedSettings.Server(type = "irrelevant", settings = emptyMap(), extras = emptyMap())
    }
}

private class FakePlatformConfigProvider : PlatformConfigProvider {
    override val isDebug: Boolean
        get() = true
}
