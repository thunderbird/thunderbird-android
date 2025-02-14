package net.discdd.k9.onboarding

import app.k9mail.feature.account.setup.domain.usecase.CreateAccount
import net.discdd.k9.onboarding.repository.AuthRepositoryImpl
import net.discdd.k9.onboarding.repository.AuthRepository
import net.discdd.k9.onboarding.ui.login.LoginViewModel
import net.discdd.k9.onboarding.ui.register.RegisterViewModel
import net.discdd.k9.onboarding.util.AuthStateConfig
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val featureDddOnboardingModule: Module = module {
    single<AuthStateConfig> { AuthStateConfig(get()) }
    single<AuthRepository> { AuthRepositoryImpl(
        authStateConfig = get(),
        context = get()
    ) }

    factory<CreateAccount> {
        CreateAccount(
            accountCreator = get(),
        )
    }

    viewModel{
        LoginViewModel (
            createAccount = get(),
            authRepository = get()
        )
    }

    viewModel{
        RegisterViewModel (
            authRepository = get()
        )
    }
}
