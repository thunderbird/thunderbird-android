package net.thunderbird.feature.mail.message.list.internal

import kotlin.test.Test
import kotlin.time.Clock
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.backend.api.folder.RemoteFolderCreator
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.debugging.DebuggingSettingsPreferenceManager
import net.thunderbird.core.preference.display.DisplaySettingsPreferenceManager
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.account.profile.AccountProfileRepository
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository
import net.thunderbird.feature.mail.folder.api.data.repository.RemoteFolderQueryRepository
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.mock

class FeatureMailMessageListDiModuleTest {

    private val externalDependencies = module {
        single<Logger> { mock() }
        single<FeatureThemeProvider> { mock() }
        single<Clock> { mock() }
        single<StringsResourceManager> { mock() }
        single<GeneralSettingsManager> { mock() }
        single<DebuggingSettingsPreferenceManager> { mock() }
        single<DisplaySettingsPreferenceManager> { mock() }
        single<InteractionSettingsPreferenceManager> { mock() }
        single<LegacyAccountManager> { mock() }
        single<BackendStorageFactory> { mock() }
        single<RemoteFolderCreator.Factory> { mock() }
        single<SpecialFolderUpdater.Factory> { mock() }
        single<RemoteFolderQueryRepository> { mock() }
        single<FolderQueryRepository> { mock() }
        single<AccountProfileRepository> { mock() }
        single<DomainContract.UseCase.GetDefaultSortCriteria> { mock() }
        single<DomainContract.UseCase.UpdateSortCriteria> { mock() }
    }

    @Test
    fun `should have a complete di graph`() {
        try {
            startKoin {
                modules(featureMailMessageListDiModule, externalDependencies)
            }
        } finally {
            stopKoin()
        }
    }
}
