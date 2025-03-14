package app.k9mail.feature.navigation.drawer.domain.entity

internal data class TreeFolder(
    var value: DisplayFolder? = null,
) {
    val children: ArrayList<TreeFolder> = ArrayList()

    fun getAllUnreadMessageCount(): Int {
        var allUnreadMessageCount = 0
        for (child in children) {
            allUnreadMessageCount += child.getAllUnreadMessageCount()
        }
        return allUnreadMessageCount + (value?.unreadMessageCount ?: 0)
    }

    fun getAllStarredMessageCount(): Int {
        var allStarredMessageCount = 0
        for (child in children) {
            allStarredMessageCount += child.getAllStarredMessageCount()
        }
        return allStarredMessageCount + (value?.starredMessageCount ?: 0)
    }
}
