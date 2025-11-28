package net.thunderbird.feature.mail.message.list.impl.fakes

import net.thunderbird.feature.mail.account.api.BaseAccount

internal data class FakeAccount(
    override val uuid: String,
    override val email: String = "fake@mail.com",
) : BaseAccount {
    override val name: String?
        get() = email
}
