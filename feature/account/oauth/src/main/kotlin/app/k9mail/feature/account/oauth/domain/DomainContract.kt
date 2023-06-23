package app.k9mail.feature.account.oauth.domain

interface DomainContract {

    interface UseCase {
        fun interface SuggestServerName {
            fun suggest(protocol: String, domain: String): String
        }
    }
}
