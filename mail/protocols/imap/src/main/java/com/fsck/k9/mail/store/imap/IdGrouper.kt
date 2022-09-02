package com.fsck.k9.mail.store.imap

private const val NO_VALID_ID = -1L

internal object IdGrouper {
    fun groupIds(ids: Set<Long>): GroupedIds {
        require(ids.isNotEmpty()) { "groupIds() must be called with non-empty set of IDs" }

        if (ids.size < 2) return GroupedIds(ids, emptyList())

        val orderedIds = ids.toSortedSet()
        val firstId = orderedIds.first()

        val remainingIds = mutableSetOf(firstId)
        val idGroups = mutableListOf<ContiguousIdGroup>()

        var previousId = firstId
        var currentIdGroupStart = NO_VALID_ID
        var currentIdGroupEnd = NO_VALID_ID
        for (currentId in orderedIds.asSequence().drop(1)) {
            if (previousId + 1L == currentId) {
                if (currentIdGroupStart == NO_VALID_ID) {
                    remainingIds.remove(previousId)
                    currentIdGroupStart = previousId
                    currentIdGroupEnd = currentId
                } else {
                    currentIdGroupEnd = currentId
                }
            } else {
                if (currentIdGroupStart != NO_VALID_ID) {
                    idGroups.add(ContiguousIdGroup(currentIdGroupStart, currentIdGroupEnd))
                    currentIdGroupStart = NO_VALID_ID
                }
                remainingIds.add(currentId)
            }

            previousId = currentId
        }

        if (currentIdGroupStart != NO_VALID_ID) {
            idGroups.add(ContiguousIdGroup(currentIdGroupStart, currentIdGroupEnd))
        }

        return GroupedIds(remainingIds, idGroups)
    }
}

internal class GroupedIds(@JvmField val ids: Set<Long>, @JvmField val idGroups: List<ContiguousIdGroup>) {
    init {
        require(ids.isNotEmpty() || idGroups.isNotEmpty()) { "Must have at least one ID" }
    }
}

internal class ContiguousIdGroup(val start: Long, val end: Long) {
    init {
        require(start < end) { "start >= end" }
    }

    override fun toString(): String {
        return "$start:$end"
    }
}
