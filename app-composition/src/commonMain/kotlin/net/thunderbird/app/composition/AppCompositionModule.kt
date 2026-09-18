package net.thunderbird.app.composition

import net.thunderbird.app.composition.feature.mail.mailCompositionModule
import net.thunderbird.feature.account.core.featureAccountCoreModule
import org.koin.core.module.Module
import org.koin.dsl.module

val appCompositionModule: Module = module {
    includes(
        featureAccountCoreModule,
        mailCompositionModule,
    )
}
