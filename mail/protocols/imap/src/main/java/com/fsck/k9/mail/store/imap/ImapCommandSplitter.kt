package com.fsck.k9.mail.store.imap

internal object ImapCommandSplitter {
    fun splitCommand(prefix: String, suffix: String, groupedIds: GroupedIds, lengthLimit: Int): List<String> {
        val commands = mutableListOf<String>()
        val workingIdSet = sortedSetOf<Long>().apply { addAll(groupedIds.ids) }
        val workingIdGroups = groupedIds.idGroups.toMutableList()
        val suffixLength = suffix.length
        val staticCommandLength = prefix.length + suffixLength + 2
        while (workingIdSet.isNotEmpty() || workingIdGroups.isNotEmpty()) {
            val commandBuilder = StringBuilder(prefix).append(' ')
            var length = staticCommandLength
            while (length < lengthLimit) {
                val (appended, newLength) = appendNextItem(
                    commandBuilder,
                    workingIdSet,
                    workingIdGroups,
                    length,
                    lengthLimit,
                )
                length = newLength
                if (!appended) break
            }
            if (suffixLength != 0) {
                // Replace the last comma with a space
                commandBuilder.setCharAt(commandBuilder.length - 1, ' ')
                commandBuilder.append(suffix)
            } else {
                // Remove last comma
                commandBuilder.setLength(commandBuilder.length - 1)
            }
            commands.add(commandBuilder.toString())
        }
        return commands
    }
    private fun appendNextItem(
        commandBuilder: StringBuilder,
        workingIdSet: MutableSet<Long>,
        workingIdGroups: MutableList<ContiguousIdGroup>,
        currentLength: Int,
        lengthLimit: Int,
    ): Pair<Boolean, Int> {
        return when {
            workingIdSet.isNotEmpty() -> {
                val id = workingIdSet.first()
                val idString = id.toString()
                val newLength = currentLength + idString.length + 1
                if (newLength >= lengthLimit) {
                    false to currentLength
                } else {
                    commandBuilder.append(idString).append(',')
                    workingIdSet.remove(id)
                    true to newLength
                }
            }
            workingIdGroups.isNotEmpty() -> {
                val idGroup = workingIdGroups.first()
                val idGroupString = idGroup.toString()
                val newLength = currentLength + idGroupString.length + 1
                if (newLength >= lengthLimit) {
                    false to currentLength
                } else {
                    commandBuilder.append(idGroupString).append(',')
                    workingIdGroups.remove(idGroup)
                    true to newLength
                }
            }
            else -> false to currentLength
        }
    }
}
