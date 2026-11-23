package net.thunderbird.feature.changelog.internal.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.changelog.internal.ChangeLogManager
import net.thunderbird.feature.changelog.internal.ChangelogVersionHistory
import net.thunderbird.feature.changelog.internal.ui.RecentChangesContract.Event
import net.thunderbird.feature.changelog.internal.ui.RecentChangesContract.State
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

// ChangeLogManager and ChangelogVersionHistory are final classes that need a Context and storage, so they are mocked.
@OptIn(ExperimentalCoroutinesApi::class)
class RecentChangesViewModelTest {

    private val changelogVersionHistory = mock<ChangelogVersionHistory>()
    private val changeLogManager = mock<ChangeLogManager> {
        on { changelogFlow } doReturn flowOf(changelogVersionHistory)
    }
    private val generalSettingsManager = FakeGeneralSettingsManager()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should show the hint after an update when recent changes are enabled`() {
        givenChangelog(isFirstRun = true, isFirstRunEver = false)
        generalSettingsManager.setShowRecentChanges(true)

        val testSubject = createTestSubject()

        assertThat(testSubject.state.value).isEqualTo(State(showRecentChangesHint = true))
    }

    @Test
    fun `should not show the hint when recent changes are disabled`() {
        givenChangelog(isFirstRun = true, isFirstRunEver = false)
        generalSettingsManager.setShowRecentChanges(false)

        val testSubject = createTestSubject()

        assertThat(testSubject.state.value).isEqualTo(State(showRecentChangesHint = false))
    }

    @Test
    fun `should not show the hint on the first run ever`() {
        givenChangelog(isFirstRun = true, isFirstRunEver = true)
        generalSettingsManager.setShowRecentChanges(true)

        val testSubject = createTestSubject()

        assertThat(testSubject.state.value).isEqualTo(State(showRecentChangesHint = false))
    }

    @Test
    fun `should not show the hint when the version did not change`() {
        givenChangelog(isFirstRun = false, isFirstRunEver = false)
        generalSettingsManager.setShowRecentChanges(true)

        val testSubject = createTestSubject()

        assertThat(testSubject.state.value).isEqualTo(State(showRecentChangesHint = false))
    }

    @Test
    fun `should write the current version when the hint is dismissed`() {
        givenChangelog(isFirstRun = true, isFirstRunEver = false)
        generalSettingsManager.setShowRecentChanges(true)
        val testSubject = createTestSubject()

        testSubject.event(Event.OnRecentChangesHintDismissed)

        verify(changeLogManager).writeCurrentVersion()
    }

    private fun createTestSubject() = RecentChangesViewModel(
        generalSettingsManager = generalSettingsManager,
        changeLogManager = changeLogManager,
    )

    private fun givenChangelog(isFirstRun: Boolean, isFirstRunEver: Boolean) {
        whenever(changelogVersionHistory.isFirstRun()).thenReturn(isFirstRun)
        whenever(changelogVersionHistory.isFirstRunEver()).thenReturn(isFirstRunEver)
    }
}

private class FakeGeneralSettingsManager : GeneralSettingsManager {
    private val generalSettings =
        MutableStateFlow(GeneralSettings(platformConfigProvider = FakePlatformConfigProvider()))

    fun setShowRecentChanges(showRecentChanges: Boolean) {
        generalSettings.value = generalSettings.value.let { settings ->
            settings.copy(
                display = settings.display.copy(
                    miscSettings = settings.display.miscSettings.copy(showRecentChanges = showRecentChanges),
                ),
            )
        }
    }

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
