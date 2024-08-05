package app.k9mail.legacy.account

interface BaseAccount {
    val uuid: String
    val name: String?
    val email: String
}
