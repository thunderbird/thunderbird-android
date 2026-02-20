package net.thunderbird.feature.applock.impl

import androidx.biometric.BiometricManager
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.applock.api.AppLockAuthenticatorFactory
import net.thunderbird.feature.applock.api.AppLockCoordinator
import net.thunderbird.feature.applock.api.AppLockGate
import net.thunderbird.feature.applock.api.AppLockSettingsNavigation
import net.thunderbird.feature.applock.impl.data.AppLockConfigStore
import net.thunderbird.feature.applock.impl.domain.BiometricAuthenticatorFactory
import net.thunderbird.feature.applock.impl.domain.DefaultAppLockCoordinator
import net.thunderbird.feature.applock.impl.domain.DefaultAppLockLifecycleHandler
import net.thunderbird.feature.applock.impl.domain.DefaultBiometricAvailabilityChecker
import net.thunderbird.feature.applock.impl.ui.DefaultAppLockGateFactory
import net.thunderbird.feature.applock.impl.ui.settings.AppLockSettingsViewModel
import net.thunderbird.feature.applock.impl.ui.settings.DefaultAppLockSettingsNavigation
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin DI module for the app lock feature.
 *
 * Public API:
 * - [AppLockCoordinator] - Main entry point for app lock functionality
 * - [AppLockGate.Factory] - Factory to create lifecycle-aware app lock handlers
 */
val featureAppLockModule: Module = module {
    // Internal components
    single {
        AppLockConfigStore(
            context = androidContext(),
        )
    }

    single {
        DefaultBiometricAvailabilityChecker(
            biometricManager = BiometricManager.from(androidApplication()),
        )
    }

    single {
        DefaultAppLockLifecycleHandler(
            application = androidApplication(),
        )
    }

    single {
        DefaultAppLockCoordinator(
            configRepository = get<AppLockConfigStore>(),
            availability = get<DefaultBiometricAvailabilityChecker>(),
            lifecycleHandler = get<DefaultAppLockLifecycleHandler>(),
        )
    }

    // Public API - only this is exposed for injection by other modules
    single<AppLockCoordinator> { get<DefaultAppLockCoordinator>() }
    single<AppLockAuthenticatorFactory> { BiometricAuthenticatorFactory() }

    // App lock gate factory for activities
    single<AppLockGate.Factory> {
        DefaultAppLockGateFactory(
            coordinator = get(),
            authenticatorFactory = get(),
            themeProvider = get<FeatureThemeProvider>(),
        )
    }

    // Settings UI
    viewModel { AppLockSettingsViewModel(coordinator = get()) }
    single<AppLockSettingsNavigation> { DefaultAppLockSettingsNavigation() }
}
