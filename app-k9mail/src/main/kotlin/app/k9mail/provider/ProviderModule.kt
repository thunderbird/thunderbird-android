package app.k9mail.provider

import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.core.common.provider.BrandNameProvider
import app.k9mail.core.ui.theme.api.FeatureThemeProvider
import app.k9mail.core.ui.theme.api.ThemeProvider
import app.k9mail.provider.K9ThemeProvider
import com.fsck.k9.preferences.FilePrefixProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.binds
import org.koin.dsl.module

internal val providerModule = module {
    single {
        K9AppNameProvider(androidContext())
    } binds arrayOf(AppNameProvider::class, BrandNameProvider::class, FilePrefixProvider::class)

    single<ThemeProvider> { K9ThemeProvider() }

    single<FeatureThemeProvider> { K9FeatureThemeProvider() }
}
