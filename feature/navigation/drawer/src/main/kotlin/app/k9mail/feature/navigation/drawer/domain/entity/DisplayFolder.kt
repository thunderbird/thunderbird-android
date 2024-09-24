package app.k9mail.feature.navigation.drawer.domain.entity

interface DisplayFolder {
    val id: String
    val unreadMessageCount: Int
    val starredMessageCount: Int
}
