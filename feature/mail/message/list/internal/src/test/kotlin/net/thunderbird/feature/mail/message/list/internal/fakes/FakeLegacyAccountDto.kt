package net.thunderbird.feature.mail.message.list.internal.fakes

import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.account.AccountIdFactory

@Suppress("TestFunctionName", "ktlint:standard:function-naming")
fun FakeLegacyAccountDto(
    uuid: String = AccountIdFactory.create().toString(),
    name: String = "fake",
    email: String = "fake@mail.com",
): LegacyAccountDto = LegacyAccountDto(uuid = uuid).apply {
    this.name = name
    identities = mutableListOf(Identity(email = email))
    this.email = email
}
