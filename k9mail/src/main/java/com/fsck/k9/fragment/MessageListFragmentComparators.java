package com.fsck.k9.fragment;

import android.database.Cursor;

import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.mail.Message;

import java.util.Comparator;
import java.util.List;

/**
 * A set of {@link Comparator} classes used for {@link Cursor} data comparison.
 */
public class MessageListFragmentComparators {
    /**
     * Reverses the result of a {@link Comparator}.
     *
     * @param <T>
     */
    public static class ReverseComparator<T> implements Comparator<T> {
        private Comparator<T> mDelegate;

        /**
         * @param delegate
         *         Never {@code null}.
         */
        public ReverseComparator(final Comparator<T> delegate) {
            mDelegate = delegate;
        }

        @Override
        public int compare(final T object1, final T object2) {
            // arg1 & 2 are mixed up, this is done on purpose
            return mDelegate.compare(object2, object1);
        }
    }

    /**
     * Chains comparator to find a non-0 result.
     *
     * @param <T>
     */
    public static class ComparatorChain<T> implements Comparator<T> {
        private List<Comparator<T>> mChain;

        /**
         * @param chain
         *         Comparator chain. Never {@code null}.
         */
        public ComparatorChain(final List<Comparator<T>> chain) {
            mChain = chain;
        }

        @Override
        public int compare(T object1, T object2) {
            int result = 0;
            for (final Comparator<T> comparator : mChain) {
                result = comparator.compare(object1, object2);
                if (result != 0) {
                    break;
                }
            }
            return result;
        }
    }

    public static class ReverseIdComparator implements Comparator<Cursor> {
        private int mIdColumn = -1;

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            if (mIdColumn == -1) {
                mIdColumn = cursor1.getColumnIndex("_id");
            }
            long o1Id = cursor1.getLong(mIdColumn);
            long o2Id = cursor2.getLong(mIdColumn);
            return (o1Id > o2Id) ? -1 : 1;
        }
    }

    public static class AttachmentComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            int o1HasAttachment = (cursor1.getInt(MessageListFragment.ATTACHMENT_COUNT_COLUMN) > 0) ? 0 : 1;
            int o2HasAttachment = (cursor2.getInt(MessageListFragment.ATTACHMENT_COUNT_COLUMN) > 0) ? 0 : 1;
            return o1HasAttachment - o2HasAttachment;
        }
    }

    public static class FlaggedComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            int o1IsFlagged = (cursor1.getInt(MessageListFragment.FLAGGED_COLUMN) == 1) ? 0 : 1;
            int o2IsFlagged = (cursor2.getInt(MessageListFragment.FLAGGED_COLUMN) == 1) ? 0 : 1;
            return o1IsFlagged - o2IsFlagged;
        }
    }

    public static class UnreadComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            int o1IsUnread = cursor1.getInt(MessageListFragment.READ_COLUMN);
            int o2IsUnread = cursor2.getInt(MessageListFragment.READ_COLUMN);
            return o1IsUnread - o2IsUnread;
        }
    }

    public static class DateComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            long o1Date = cursor1.getLong(MessageListFragment.DATE_COLUMN);
            long o2Date = cursor2.getLong(MessageListFragment.DATE_COLUMN);
            if (o1Date < o2Date) {
                return -1;
            } else if (o1Date == o2Date) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    public static class ArrivalComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            long o1Date = cursor1.getLong(MessageListFragment.INTERNAL_DATE_COLUMN);
            long o2Date = cursor2.getLong(MessageListFragment.INTERNAL_DATE_COLUMN);
            if (o1Date == o2Date) {
                return 0;
            } else if (o1Date < o2Date) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public static class SubjectComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            String subject1 = cursor1.getString(MessageListFragment.SUBJECT_COLUMN);
            String subject2 = cursor2.getString(MessageListFragment.SUBJECT_COLUMN);

            if (subject1 == null) {
                return (subject2 == null) ? 0 : -1;
            } else if (subject2 == null) {
                return 1;
            }

            return subject1.compareToIgnoreCase(subject2);
        }
    }

    public static class SenderComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            String sender1 = MessageListFragment.getSenderAddressFromCursor(cursor1);
            String sender2 = MessageListFragment.getSenderAddressFromCursor(cursor2);

            if (sender1 == null && sender2 == null) {
                return 0;
            } else if (sender1 == null) {
                return 1;
            } else if (sender2 == null) {
                return -1;
            } else {
                return sender1.compareToIgnoreCase(sender2);
            }
        }
    }
}
