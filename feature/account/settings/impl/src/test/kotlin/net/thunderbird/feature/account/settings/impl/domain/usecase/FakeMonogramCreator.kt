package net.thunderbird.feature.account.settings.impl.domain.usecase

import net.thunderbird.feature.account.avatar.AvatarMonogramCreator

internal class FakeMonogramCreator : AvatarMonogramCreator {
    override fun create(name: String?, email: String?): String {
        return name?.take(2)?.uppercase() ?: "XX"
    }
}
