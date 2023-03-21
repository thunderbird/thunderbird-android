package com.fsck.k9.mail.store.imap

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.junit.Test

class IdGrouperTest {
    @Test
    fun `groupIds() with single contiguous group`() {
        val ids = setOf(1L, 2L, 3L)

        val groupedIds = IdGrouper.groupIds(ids)

        assertThat(groupedIds.ids).isEmpty()
        assertThat(groupedIds.idGroups.mapToString()).containsExactly("1:3")
    }

    @Test
    fun `groupIds() without contiguous group`() {
        val ids = setOf(23L, 42L, 2L, 5L)

        val groupedIds = IdGrouper.groupIds(ids)

        assertThat(groupedIds.ids).isEqualTo(ids)
        assertThat(groupedIds.idGroups).isEmpty()
    }

    @Test
    fun `groupIds() with multiple contiguous groups`() {
        val ids = setOf(1L, 3L, 4L, 5L, 6L, 10L, 12L, 13L, 14L, 23L)

        val groupedIds = IdGrouper.groupIds(ids)

        assertThat(groupedIds.ids).containsExactlyInAnyOrder(1L, 10L, 23L)
        assertThat(groupedIds.idGroups.mapToString()).containsExactly("3:6", "12:14")
    }

    @Test
    fun `groupIds() with single ID`() {
        val ids = setOf(23L)

        val groupedIds = IdGrouper.groupIds(ids)

        assertThat(groupedIds.ids).containsExactlyInAnyOrder(23L)
        assertThat(groupedIds.idGroups).isEmpty()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `groupIds() with empty set should throw`() {
        IdGrouper.groupIds(emptySet())
    }
}

private fun <T> List<T>.mapToString() = map { it.toString() }
