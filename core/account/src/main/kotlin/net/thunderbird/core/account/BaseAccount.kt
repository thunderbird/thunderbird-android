package net.thunderbird.core.account

interface BaseAccount {
    val uuid: String
    val name: String?
    val email: String
}
