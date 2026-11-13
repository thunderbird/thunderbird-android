package net.thunderbird.feature.account.settings

import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.test.Test
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer
import net.thunderbird.core.file.DirectoryProvider
import net.thunderbird.core.file.FileManager
import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.file.MimeTypeResolver
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.AccountCapabilities
import net.thunderbird.feature.account.avatar.AvatarIcon
import net.thunderbird.feature.account.avatar.AvatarIconCatalog
import net.thunderbird.feature.account.avatar.AvatarImageRepository
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.account.profile.AccountProfileRepository
import net.thunderbird.feature.account.settings.api.AccountSettingsExternalContract
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock

internal class FeatureAccountSettingsDiModuleTest {

    private val externalDependencies = module {
        single<Logger> { mock() }
        single<StringsResourceManager> { mock() }
        single<AccountCapabilities> { mock() }
        single<AccountProfileRepository> { mock() }
        single<LegacyAccountRepository> { mock() }
        single<AvatarImageRepository> { mock() }
        single<MimeTypeResolver> { mock() }
        single<AvatarMonogramCreator> { mock() }
        single<FeatureFlagProvider> { mock() }
        single<AvatarIconCatalog<AvatarIcon<ImageVector>>> { mock() }
        single<ImmutableList<Int>>(named("AccountColors")) { mock() }
        single<SettingViewProvider> { mock() }
        single<AppNameProvider> { mock() }
        single<AccountSettingsExternalContract.Launcher> { mock() }

        // External inputs of the feature-flag and avatar dependencies.
        single<FileSystemManager> { mock() }
        single<PlatformConfigProvider> { mock() }
        single<FlagRegistryOverrideSerializer> { mock() }
        single<ConfigBackendProvider> { mock() }
        single<FileManager> { mock() }
        single<DirectoryProvider> { mock() }
    }

    @Test
    fun `should have a complete di graph`() {
        try {
            startKoin {
                modules(featureAccountSettingsDiModule, externalDependencies)
            }
        } finally {
            stopKoin()
        }
    }
}
