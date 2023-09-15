package app.k9mail.feature.account.edit

import app.k9mail.core.common.coreCommonModule
import app.k9mail.feature.account.oauth.featureAccountOAuthModule
import org.koin.dsl.module

val accountEditModule = module {
    includes(coreCommonModule, featureAccountOAuthModule)
}
