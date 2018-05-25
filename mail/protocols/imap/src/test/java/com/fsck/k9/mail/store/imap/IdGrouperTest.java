package com.fsck.k9.mail.store.imap;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class IdGrouperTest {
    @Test
    public void groupIds_withSingleContiguousGroup() throws Exception {
        Set<Long> ids = newSet(1L, 2L, 3L);

        IdGrouper.GroupedIds groupedIds = IdGrouper.groupIds(ids);

        assertEquals(0, groupedIds.ids.size());
        assertEquals(1, groupedIds.idGroups.size());
        assertEquals("1:3", groupedIds.idGroups.get(0).toString());
    }

    @Test
    public void groupIds_withoutContiguousGroup() throws Exception {
        Set<Long> ids = newSet(23L, 42L, 2L, 5L);

        IdGrouper.GroupedIds groupedIds = IdGrouper.groupIds(ids);

        assertEquals(ids, groupedIds.ids);
        assertEquals(0, groupedIds.idGroups.size());
    }

    @Test
    public void groupIds_withMultipleContiguousGroups() throws Exception {
        Set<Long> ids = newSet(1L, 3L, 4L, 5L, 6L, 10L, 12L, 13L, 14L, 23L);

        IdGrouper.GroupedIds groupedIds = IdGrouper.groupIds(ids);

        assertEquals(newSet(1L, 10L, 23L), groupedIds.ids);
        assertEquals(2, groupedIds.idGroups.size());
        assertEquals("3:6", groupedIds.idGroups.get(0).toString());
        assertEquals("12:14", groupedIds.idGroups.get(1).toString());
    }

    @Test
    public void groupIds_withSingleId() throws Exception {
        Set<Long> ids = newSet(23L);

        IdGrouper.GroupedIds groupedIds = IdGrouper.groupIds(ids);

        assertEquals(newSet(23L), groupedIds.ids);
        assertEquals(0, groupedIds.idGroups.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void groupIds_withEmptySet_shouldThrow() throws Exception {
        IdGrouper.groupIds(newSet());
    }

    @Test(expected = IllegalArgumentException.class)
    public void groupIds_withNullArgument_shouldThrow() throws Exception {
        IdGrouper.groupIds(null);
    }


    private static Set<Long> newSet(Long... values) {
        HashSet<Long> set = new HashSet<>(values.length);
        set.addAll(Arrays.asList(values));
        return set;
    }
}
