package net.thunderbird.feature.mail.message.list.internal.domain.usecase

import app.cash.turbine.test
import assertk.all
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.display.DisplaySettings
import net.thunderbird.core.preference.interaction.KEY_SWIPE_ACTION_LEFT
import net.thunderbird.core.preference.interaction.KEY_SWIPE_ACTION_RIGHT
import net.thunderbird.core.preference.network.NetworkSettings
import net.thunderbird.core.preference.notification.NotificationPreference
import net.thunderbird.core.preference.privacy.PrivacySettings
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeLegacyAccount
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeLegacyAccountManager

@OptIn(ExperimentalUuidApi::class)
@Suppress("MaxLineLength")
class BuildSwipeActionsTest {
    private val defaultGeneralSettings
        get() = GeneralSettings(
            display = DisplaySettings(),
            network = NetworkSettings(),
            notification = NotificationPreference(),
            privacy = PrivacySettings(),
            platformConfigProvider = FakePlatformConfigProvider(),
        )

    @Test
    fun `invoke should return empty map when empty account ids is provided`() = runTest {
        // Arrange
        val testSubject = createTestSubject(accounts = emptyList())

        // Act
        val subject = testSubject()
        subject.test {
            // Assert
            val actions = awaitItem()
            assertThat(actions).isEmpty()
        }
    }

    @Test
    fun `invoke should return map with SwipeActions(ToggleRead, ToggleSelection) when no user preference is stored`() =
        runTest {
            // Arrange
            val id = AccountIdFactory.create()
            val ids = listOf(id)
            val testSubject = createTestSubject(accountsIds = ids)

            // Act
            val subject = testSubject()
            subject.test {
                // Assert
                val actions = awaitItem()
                assertThat(actions).all {
                    hasSize(size = 1)
                    containsOnly(
                        id to SwipeActions(
                            leftAction = SwipeAction.ToggleRead,
                            rightAction = SwipeAction.ToggleSelection,
                        ),
                    )
                }
            }
        }

    @Test
    fun `invoke should return map with multiple keys when multiple accounts`() = runTest {
        // Arrange
        val accountsSize = 10
        val ids = List(size = accountsSize) { AccountIdFactory.create() }
        val testSubject = createTestSubject(accountsIds = ids)

        // Act
        val subject = testSubject()
        subject.test {
            // Assert
            val actions = awaitItem()
            assertThat(actions).all {
                hasSize(accountsSize)
                containsOnly(
                    elements = ids
                        .associateWith {
                            SwipeActions(
                                leftAction = SwipeAction.ToggleRead,
                                rightAction = SwipeAction.ToggleSelection,
                            )
                        }
                        .map { it.key to it.value }
                        .toTypedArray(),
                )
            }
        }
    }

    @Test
    fun `invoke should return map with SwipeActions(None, ToggleSelection) when left action is stored as None but right isn't'`() =
        runTest {
            // Arrange
            val id = AccountIdFactory.create()
            val ids = listOf(id)
            val testSubject = createTestSubject(
                accountsIds = ids,
                storageValues = mapOf(
                    KEY_SWIPE_ACTION_LEFT to SwipeAction.None,
                ),
            )

            // Act
            val subject = testSubject()
            subject.test {
                // Assert
                val actions = awaitItem()
                assertThat(actions).all {
                    hasSize(size = 1)
                    containsOnly(
                        id to SwipeActions(
                            leftAction = SwipeAction.None,
                            rightAction = SwipeAction.ToggleSelection,
                        ),
                    )
                }
            }
        }

    @Test
    fun `invoke should return map with SwipeActions(ToggleRead, Delete) when left action isn't stored but right is stored as Delete`() =
        runTest {
            // Arrange
            val id = AccountIdFactory.create()
            val ids = listOf(id)
            val testSubject = createTestSubject(
                accountsIds = ids,
                storageValues = mapOf(
                    KEY_SWIPE_ACTION_RIGHT to SwipeAction.Delete,
                ),
            )

            // Act
            val subject = testSubject()
            subject.test {
                // Assert
                val actions = awaitItem()
                assertThat(actions).all {
                    hasSize(size = 1)
                    containsOnly(
                        id to SwipeActions(
                            leftAction = SwipeAction.ToggleRead,
                            rightAction = SwipeAction.Delete,
                        ),
                    )
                }
            }
        }

    @Test
    fun `invoke should return map with SwipeActions(Archive, Archive) when both stored actions are Archive, account isn't pop3 and has archive folder`() =
        runTest {
            // Arrange
            val id = AccountIdFactory.create()
            val testSubject = createTestSubject(
                accounts = listOf(FakeLegacyAccount(id = id, archiveFolderId = 123)),
                storageValues = mapOf(
                    KEY_SWIPE_ACTION_LEFT to SwipeAction.Archive,
                    KEY_SWIPE_ACTION_RIGHT to SwipeAction.Archive,
                ),
            )

            // Act
            val subject = testSubject()
            subject.test {
                // Assert
                val actions = awaitItem()
                assertThat(actions).all {
                    hasSize(size = 1)
                    containsOnly(
                        id to SwipeActions(
                            leftAction = SwipeAction.Archive,
                            rightAction = SwipeAction.Archive,
                        ),
                    )
                }
            }
        }

    @Test
    fun `invoke should return map with SwipeActions(ArchiveDisabled, ArchiveDisabled) when both stored actions are Archive, account is pop3`() =
        runTest {
            // Arrange
            val id = AccountIdFactory.create()
            val testSubject = createTestSubject(
                accounts = listOf(FakeLegacyAccount(id = id, incomingServerType = Protocols.POP3)),
                storageValues = mapOf(
                    KEY_SWIPE_ACTION_LEFT to SwipeAction.Archive,
                    KEY_SWIPE_ACTION_RIGHT to SwipeAction.Archive,
                ),
            )

            // Act
            val subject = testSubject()
            subject.test {
                // Assert
                val actions = awaitItem()
                assertThat(actions).all {
                    hasSize(size = 1)
                    containsOnly(
                        id to SwipeActions(
                            leftAction = SwipeAction.ArchiveDisabled,
                            rightAction = SwipeAction.ArchiveDisabled,
                        ),
                    )
                }
            }
        }

    @Test
    fun `invoke should return map with SwipeActions(ArchiveSetupArchiveFolder, ArchiveSetupArchiveFolder) when actions stored as Archive, account isn't pop3, has not archive folder and shouldShowSetupArchiveFolderDialog is true`() =
        runTest {
            // Arrange
            val id = AccountIdFactory.create()
            val ids = listOf(id)
            val testSubject = createTestSubject(
                initialGeneralSettings = defaultGeneralSettings.copy(
                    display = defaultGeneralSettings.display.copy(
                        miscSettings = defaultGeneralSettings.display.miscSettings.copy(
                            shouldShowSetupArchiveFolderDialog = true,
                        ),
                    ),
                ),
                accountsIds = ids,
                storageValues = mapOf(
                    KEY_SWIPE_ACTION_LEFT to SwipeAction.Archive,
                    KEY_SWIPE_ACTION_RIGHT to SwipeAction.Archive,
                ),
            )

            // Act
            val subject = testSubject()
            subject.test {
                // Assert
                val actions = awaitItem()
                assertThat(actions).all {
                    hasSize(size = 1)
                    containsOnly(
                        id to SwipeActions(
                            leftAction = SwipeAction.ArchiveSetupArchiveFolder,
                            rightAction = SwipeAction.ArchiveSetupArchiveFolder,
                        ),
                    )
                }
            }
        }

    @Test
    fun `invoke should return map with different SwipeAction Archive when multiple accounts that includes pop3 accounts or accounts without archive folder`() =
        runTest {
            // Arrange
            val idPop3 = AccountIdFactory.create()
            val accountPop3 = FakeLegacyAccount(id = idPop3, incomingServerType = Protocols.POP3)
            val idWithoutArchiveFolder = AccountIdFactory.create()
            val accountWithoutArchiveFolder = FakeLegacyAccount(id = idWithoutArchiveFolder, archiveFolderId = null)
            val idWithArchiveFolder = AccountIdFactory.create()
            val accountWithArchiveFolder = FakeLegacyAccount(id = idWithArchiveFolder, archiveFolderId = 123)
            val accounts = listOf(accountPop3, accountWithoutArchiveFolder, accountWithArchiveFolder)
            val testSubject = createTestSubject(
                initialGeneralSettings = defaultGeneralSettings.copy(
                    display = defaultGeneralSettings.display.copy(
                        miscSettings = defaultGeneralSettings.display.miscSettings.copy(
                            shouldShowSetupArchiveFolderDialog = true,
                        ),
                    ),
                ),
                accounts = accounts,
                storageValues = mapOf(
                    KEY_SWIPE_ACTION_LEFT to SwipeAction.Archive,
                    KEY_SWIPE_ACTION_RIGHT to SwipeAction.Archive,
                ),
            )

            // Act
            val subject = testSubject()
            subject.test {
                // Assert
                val actions = awaitItem()
                expectNoEvents()
                assertThat(actions).all {
                    hasSize(size = 3)
                    containsOnly(
                        idPop3 to SwipeActions(
                            leftAction = SwipeAction.ArchiveDisabled,
                            rightAction = SwipeAction.ArchiveDisabled,
                        ),
                        idWithoutArchiveFolder to SwipeActions(
                            leftAction = SwipeAction.ArchiveSetupArchiveFolder,
                            rightAction = SwipeAction.ArchiveSetupArchiveFolder,
                        ),
                        idWithArchiveFolder to SwipeActions(
                            leftAction = SwipeAction.Archive,
                            rightAction = SwipeAction.Archive,
                        ),
                    )
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @JvmName("createTestSubjectByIds")
    private fun createTestSubject(
        initialGeneralSettings: GeneralSettings = defaultGeneralSettings,
        accountsIds: List<AccountId>,
        storageValues: Map<String, SwipeAction> = mapOf(),
    ): BuildSwipeActions = createTestSubject(
        initialGeneralSettings = initialGeneralSettings,
        accounts = accountsIds.map { id -> FakeLegacyAccount(id = id) },
        storageValues = storageValues,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createTestSubject(
        initialGeneralSettings: GeneralSettings = defaultGeneralSettings,
        accounts: List<LegacyAccount>,
        storageValues: Map<String, SwipeAction> = mapOf(),
    ): BuildSwipeActions = BuildSwipeActions(
        generalSettingsManager = FakeGeneralSettingsManager(
            initialGeneralSettings.let { settings ->
                if (storageValues.isNotEmpty() &&
                    (KEY_SWIPE_ACTION_LEFT in storageValues || KEY_SWIPE_ACTION_RIGHT in storageValues)
                ) {
                    val swipeActions = settings.interaction.swipeActions
                    settings.copy(
                        interaction = settings.interaction.copy(
                            swipeActions = swipeActions.copy(
                                leftAction = storageValues[KEY_SWIPE_ACTION_LEFT] ?: swipeActions.leftAction,
                                rightAction = storageValues[KEY_SWIPE_ACTION_RIGHT] ?: swipeActions.rightAction,
                            ),
                        ),
                    )
                } else {
                    settings
                }
            },
        ),
        accountManager = FakeLegacyAccountManager(accounts = accounts),
        mainDispatcher = UnconfinedTestDispatcher(),
    )
}

private class FakeGeneralSettingsManager(
    initialGeneralSettings: GeneralSettings,
) : GeneralSettingsManager {
    private val generalSettings = MutableStateFlow(initialGeneralSettings)

    @Deprecated(
        message = "Use PreferenceManager<GeneralSettings>.getConfig() instead",
        replaceWith = ReplaceWith("getConfig()"),
    )
    override fun getSettings(): GeneralSettings = generalSettings.value

    @Deprecated(
        message = "Use PreferenceManager<GeneralSettings>.getConfigFlow() instead",
        replaceWith = ReplaceWith("getConfigFlow()"),
    )
    override fun getSettingsFlow(): Flow<GeneralSettings> = generalSettings

    override fun save(config: GeneralSettings) {
        error("not implemented")
    }

    override fun getConfig(): GeneralSettings = generalSettings.value

    override fun getConfigFlow(): Flow<GeneralSettings> = generalSettings
}

private class FakePlatformConfigProvider : PlatformConfigProvider {
    override val isDebug: Boolean
        get() = true
}
