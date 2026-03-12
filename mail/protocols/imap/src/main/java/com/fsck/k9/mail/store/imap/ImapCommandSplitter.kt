package com.fsck.k9.mail.store.imap

import java.util.ArrayList
import java.util.TreeSet

internal object ImapCommandSplitter {
    fun splitCommand(prefix: String, suffix: String, groupedIds: GroupedIds, lengthLimit: Int): List<String> {
        val commands: MutableList<String> = ArrayList()
        val workingIdSet: MutableSet<Long> = TreeSet(groupedIds.ids)
        val workingIdGroups: MutableList<ContiguousIdGroup> = ArrayList(groupedIds.idGroups)

        val suffixLength = suffix.length
        val staticCommandLength = prefix.length + suffixLength + 2
        while (workingIdSet.isNotEmpty() || workingIdGroups.isNotEmpty()) {
            val commandBuilder = StringBuilder(prefix).append(' ')
            var length = staticCommandLength
            while (length < lengthLimit) {
                if (workingIdSet.isNotEmpty()) {
                    val id = workingIdSet.iterator().next()
                    val idString = id.toString()

                    length += idString.length + 1
                    if (length >= lengthLimit) {
                        break
                    }

                    commandBuilder.append(idString).append(',')
                    workingIdSet.remove(id)
                } else if (workingIdGroups.isNotEmpty()) {
                    val idGroup = workingIdGroups.iterator().next()
                    val idGroupString = idGroup.toString()

                    length += idGroupString.length + 1
                    if (length >= lengthLimit) {
                        break
                    }

                    commandBuilder.append(idGroupString).append(',')
                    workingIdGroups.remove(idGroup)
                } else {
                    break
                }
            }

            if (suffixLength != 0) {
                // Replace the last comma with a space
                commandBuilder.setCharAt(commandBuilder.length - 1, ' ')
                commandBuilder.append(suffix)
            } else {
                // Remove last comma
                commandBuilder.setLength(commandBuilder.length - 1)
            }

            val command = commandBuilder.toString()
            commands.add(command)
        }

        return commands
    }
}
