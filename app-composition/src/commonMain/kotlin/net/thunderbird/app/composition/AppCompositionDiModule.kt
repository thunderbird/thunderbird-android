package net.thunderbird.app.composition

import net.thunderbird.app.composition.core.coreCompositionDiModule
import net.thunderbird.app.composition.feature.mail.mailCompositionDiModule
import net.thunderbird.feature.account.core.featureAccountCoreDiModule
import net.thunderbird.feature.notification.impl.featureNotificationDiModule
import org.koin.core.module.Module
import org.koin.dsl.module

val appCompositionDiModule: Module = module {
    includes(
        coreCompositionDiModule,
        featureAccountCoreDiModule,
        mailCompositionDiModule,
        featureNotificationDiModule,
    )
}
