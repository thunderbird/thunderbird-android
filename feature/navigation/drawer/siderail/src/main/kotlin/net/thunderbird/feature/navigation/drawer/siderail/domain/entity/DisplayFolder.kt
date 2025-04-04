package net.thunderbird.feature.navigation.drawer.siderail.domain.entity

internal interface DisplayFolder {
    val id: String
    val unreadMessageCount: Int
    val starredMessageCount: Int
}
