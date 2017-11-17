/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fsck.k9.fragment;

import android.content.Context;
import android.database.Cursor;
import android.widget.CursorAdapter;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import static com.fsck.k9.fragment.MLFProjectionInfo.DATE_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.INTERNAL_DATE_COLUMN;

import java.util.Calendar;


/**
 * A class to calculate the catagory of the date
 */
public class DateGroups {

    private static final int GROUP_NONE                 = 0;
    private static final int GROUP_TODAY                = 1;
    private static final int GROUP_YESTERDAY            = 2;
    private static final int GROUP_DAY_BEFORE_YESTERDAY = 3;
    private static final int GROUP_LAST_WEEK            = 11;
    private static final int GROUP_TWO_WEEKS_AGO        = 12;
    private static final int GROUP_THREE_WEEKS_AGO      = 13;
    private static final int GROUP_LAST_MONTH           = 14;
    private static final int GROUP_OLDER                = 15;

    private String dateGroupNames[] = new String[20];

    private int[] dateGroupIds = null;
    private Account.SortType sortType;
    private CursorAdapter cursorAdapter = null;

    DateGroups(Context context, MessageListAdapter adapter, Account.SortType sorttype ) {

        sortType = sorttype;
        cursorAdapter = adapter;

        dateGroupNames[GROUP_TODAY] = context.getString(R.string.message_list_separator_today);
        dateGroupNames[GROUP_YESTERDAY] = context.getString(R.string.message_list_separator_yesterday);
        dateGroupNames[GROUP_DAY_BEFORE_YESTERDAY] = context.getString(R.string.message_list_separator_day_before_yesterday);
        dateGroupNames[GROUP_LAST_WEEK] = context.getString(R.string.message_list_separator_last_week);
        dateGroupNames[GROUP_TWO_WEEKS_AGO] = context.getString(R.string.message_list_separator_two_weeks);
        dateGroupNames[GROUP_THREE_WEEKS_AGO] = context.getString(R.string.message_list_separator_three_weeks);
        dateGroupNames[GROUP_LAST_MONTH] = context.getString(R.string.message_list_separator_last_month);
        dateGroupNames[GROUP_OLDER] = context.getString(R.string.message_list_separator_older);
    }

    void addDateGroups( int[] groups ){
        dateGroupIds = groups;
    }

    String getDateGroupName(int position) {
        String groupName;
        Cursor cursor = (Cursor) cursorAdapter.getItem(position);

        if ((dateGroupIds[position] > GROUP_DAY_BEFORE_YESTERDAY) && (dateGroupIds[position] < GROUP_LAST_WEEK)) {
            if (sortType == Account.SortType.SORT_ARRIVAL) {
                groupName = (String) android.text.format.DateFormat.format("EEEE", cursor.getLong(INTERNAL_DATE_COLUMN));
            } else {
                groupName = (String) android.text.format.DateFormat.format("EEEE", cursor.getLong(DATE_COLUMN));
            }
        } else {
            groupName = dateGroupNames[dateGroupIds[position]];
        }

        return groupName;
    }

    private int getSundayOffset()
    {
        int sundayOffset = 0;
        Calendar calendar = Calendar.getInstance();
        for (int j = 0; j < 7; j++) {
            calendar.add(Calendar.DATE, -1);
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                sundayOffset = j + 1;
                break;
            }
        }
        return sundayOffset;
    }

    int getDateGroup(long date) {
        final int DATE_TODAY                = 0;
        final int DATE_YESTERDAY            = 1;
        final int DATE_DAY_BEFORE_YESTERDAY = 2;
        final int DATE_START_OF_WEEKDAYS    = 3;
        final int DATE_LAST_WEEK            = 7;
        final int DATE_TWO_WEEKS_AGO        = 14;
        final int DATE_THREE_WEEKS_AGO      = 21;
        final int DATE_LAST_MONTH           = 1;

        if (date != 0) {
            int category = 0;

            int sundayOffset = getSundayOffset();

            Calendar messageDate = Calendar.getInstance();
            messageDate.setTimeInMillis(date);

            long dateDifference = daysBetween(messageDate, Calendar.getInstance());
            int monthDifference = Calendar.getInstance().get( Calendar.MONTH) - messageDate.get( Calendar.MONTH );

            if (dateDifference == DATE_TODAY) {
                category = GROUP_TODAY;
            } else if (dateDifference == DATE_YESTERDAY) {
                category = GROUP_YESTERDAY;
            } else if (dateDifference == DATE_DAY_BEFORE_YESTERDAY) {
                category = GROUP_DAY_BEFORE_YESTERDAY;
            } else if ((sundayOffset > 1) && (dateDifference <= sundayOffset)) {
                category = messageDate.get(Calendar.DAY_OF_WEEK) + DATE_START_OF_WEEKDAYS;
            } else if ((dateDifference - sundayOffset) < DATE_LAST_WEEK) {
                category = GROUP_LAST_WEEK;
            } else if ((dateDifference - sundayOffset) < DATE_TWO_WEEKS_AGO) {
                category = GROUP_TWO_WEEKS_AGO;
            } else if ((dateDifference - sundayOffset) < DATE_THREE_WEEKS_AGO) {
                category = GROUP_THREE_WEEKS_AGO;
            } else if (monthDifference == DATE_LAST_MONTH) {
                category = GROUP_LAST_MONTH;
            } else {
                category = GROUP_OLDER;
            }
            return category;
        }
        return GROUP_NONE;
    }

    private final static long MILLISECS_PER_DAY = 24 * 60 * 60 * 1000;

    private long daysBetween(Calendar a, Calendar b) {
        if (a.get(Calendar.ERA) == b.get(Calendar.ERA) && a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)) {
            return 0;
        }
        Calendar a2 = (Calendar) a.clone();
        Calendar b2 = (Calendar) b.clone();
        a2.set(Calendar.HOUR_OF_DAY, 0);
        a2.set(Calendar.MINUTE, 0);
        a2.set(Calendar.SECOND, 0);
        a2.set(Calendar.MILLISECOND, 0);
        b2.set(Calendar.HOUR_OF_DAY, 0);
        b2.set(Calendar.MINUTE, 0);
        b2.set(Calendar.SECOND, 0);
        b2.set(Calendar.MILLISECOND, 0);
        long diff = a2.getTimeInMillis() - b2.getTimeInMillis();
        long days = diff / MILLISECS_PER_DAY;
        return Math.abs(days);
    }

}
