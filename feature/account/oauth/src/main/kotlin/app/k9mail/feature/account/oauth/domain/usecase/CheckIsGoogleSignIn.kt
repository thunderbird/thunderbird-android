package app.k9mail.feature.account.oauth.domain.usecase

import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract.UseCase

internal class CheckIsGoogleSignIn : UseCase.CheckIsGoogleSignIn {
    override fun execute(hostname: String): Boolean {
        for (domain in domainList) {
            if (hostname.lowercase().endsWith(domain)) {
                return true
            }
        }

        return false
    }

    private companion object {
        val domainList = listOf(
            ".gmail.com",
            ".googlemail.com",
            ".google.com",
        )
    }
}
