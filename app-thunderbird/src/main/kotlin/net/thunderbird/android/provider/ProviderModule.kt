package net.thunderbird.android.provider

import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import com.fsck.k9.preferences.FilePrefixProvider
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.core.ui.theme.api.ThemeProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.binds
import org.koin.dsl.module

internal val providerModule = module {
    single {
        TbAppNameProvider(androidContext())
    } binds arrayOf(AppNameProvider::class, BrandNameProvider::class, FilePrefixProvider::class)

    single<ThemeProvider> { TbThemeProvider() }

    single<FeatureThemeProvider> { TbFeatureThemeProvider() }

    single<NotificationIconResourceProvider> {
        TbAppIconNotificationProvider()
    }
}
