package net.thunderbird.feature.changelog.internal

import net.thunderbird.feature.navigation.changelog.api.ChangeLogMode
import net.thunderbird.feature.navigation.changelog.api.ChangelogNavigation
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val changelogUiModule = module {
    single { ChangeLogManager(context = get(), appCoroutineScope = get(named("AppCoroutineScope"))) }
    viewModel { (mode: ChangeLogMode) ->
        ChangelogViewModel(generalSettingsManager = get(), changeLogManager = get(), mode = mode)
    }
    viewModel { RecentChangesViewModel(generalSettingsManager = get(), changeLogManager = get()) }
    single<ChangelogNavigation> { DefaultChangelogNavigation() }
}
