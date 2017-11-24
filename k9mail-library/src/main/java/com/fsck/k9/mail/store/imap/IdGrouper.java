package com.fsck.k9.mail.store.imap;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


class IdGrouper {
    static GroupedIds groupIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("groupId() must be called with non-empty set of ids");
        }

        if (ids.size() < 2) {
            return new GroupedIds(ids, Collections.<ContiguousIdGroup>emptyList());
        }

        TreeSet<Long> orderedIds = new TreeSet<Long>(ids);
        Iterator<Long> orderedIdIterator = orderedIds.iterator();
        Long previousId = orderedIdIterator.next();

        TreeSet<Long> remainingIds = new TreeSet<Long>();
        remainingIds.add(previousId);
        List<ContiguousIdGroup> idGroups = new ArrayList<>();
        long currentIdGroupStart = -1L;
        long currentIdGroupEnd = -1L;
        while (orderedIdIterator.hasNext()) {
            Long currentId = orderedIdIterator.next();
            if (previousId + 1L == currentId) {
                if (currentIdGroupStart == -1L) {
                    remainingIds.remove(previousId);
                    currentIdGroupStart = previousId;
                    currentIdGroupEnd = currentId;
                } else {
                    currentIdGroupEnd = currentId;
                }
            } else {
                if (currentIdGroupStart != -1L) {
                    idGroups.add(new ContiguousIdGroup(currentIdGroupStart, currentIdGroupEnd));
                    currentIdGroupStart = -1L;
                }
                remainingIds.add(currentId);
            }

            previousId = currentId;
        }

        if (currentIdGroupStart != -1L) {
            idGroups.add(new ContiguousIdGroup(currentIdGroupStart, currentIdGroupEnd));
        }

        return new GroupedIds(remainingIds, idGroups);
    }


    static class GroupedIds {
        public final Set<Long> ids;
        public final List<ContiguousIdGroup> idGroups;


        GroupedIds(Set<Long> ids, List<ContiguousIdGroup> idGroups) {
            if (ids.isEmpty() && idGroups.isEmpty()) {
                throw new IllegalArgumentException("Must have at least one id");
            }

            this.ids = ids;
            this.idGroups = idGroups;
        }
    }

    static class ContiguousIdGroup {
        public final long start;
        public final long end;


        ContiguousIdGroup(long start, long end) {
            if (start >= end) {
                throw new IllegalArgumentException("start >= end");
            }

            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return start + ":" + end;
        }
    }
}
