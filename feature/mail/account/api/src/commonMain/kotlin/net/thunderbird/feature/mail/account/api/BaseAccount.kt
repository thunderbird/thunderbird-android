package net.thunderbird.feature.mail.account.api

interface BaseAccount {
    val uuid: String
    val name: String?
    val email: String
}
