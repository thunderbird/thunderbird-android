package com.fsck.k9.grouping;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.fsck.k9.R;

import android.content.Context;

public class DateMessageGrouper implements MessageGrouper
{

    private final Context mContext;

    public DateMessageGrouper(final Context context)
    {
        this.mContext = context;
    }

    @Override
    public <T> List<MessageGroup<T>> group(final Collection<MessageInfo<T>> messages)
    {
        final Calendar today = Calendar.getInstance(TimeZone.getDefault());
        today.set(Calendar.MILLISECOND, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.HOUR_OF_DAY, 0);

        final Calendar yesterday = (Calendar) today.clone();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);

        final Calendar thisWeek = (Calendar) today.clone();
        final int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        final int firstDayOfWeek = today.getFirstDayOfWeek();
        if (dayOfWeek > firstDayOfWeek)
        {
            thisWeek.add(Calendar.DATE, -(dayOfWeek - firstDayOfWeek));
        }
        else if (dayOfWeek < firstDayOfWeek)
        {
            thisWeek.add(Calendar.DATE, -(7 - dayOfWeek));
        }

        final Calendar lastWeek = (Calendar) thisWeek.clone();
        lastWeek.add(Calendar.WEEK_OF_YEAR, -1);

        final Calendar thisMonth = (Calendar) today.clone();
        thisMonth.set(Calendar.DAY_OF_MONTH, 1);

        final Calendar lastMonth = (Calendar) thisMonth.clone();
        lastMonth.add(Calendar.MONTH, -1);

        final Calendar thisYear = (Calendar) today.clone();
        thisYear.set(Calendar.DAY_OF_YEAR, 1);

        final List<MessageInfo<T>> todayList = new ArrayList<MessageInfo<T>>();
        final List<MessageInfo<T>> yesterdayList = new ArrayList<MessageInfo<T>>();
        final List<MessageInfo<T>> thisWeekList = new ArrayList<MessageInfo<T>>();
        final List<MessageInfo<T>> lastWeekList = new ArrayList<MessageInfo<T>>();
        final List<MessageInfo<T>> thisMonthList = new ArrayList<MessageInfo<T>>();
        final List<MessageInfo<T>> lastMonthList = new ArrayList<MessageInfo<T>>();
        final List<MessageInfo<T>> thisYearList = new ArrayList<MessageInfo<T>>();
        final List<MessageInfo<T>> earlierList = new ArrayList<MessageInfo<T>>();

        final Date todayDate = today.getTime();
        final Date yesterdayDate = yesterday.getTime();
        final Date thisWeekDate = thisWeek.getTime();
        final Date lastWeekDate = lastWeek.getTime();
        final Date thisMonthDate = thisMonth.getTime();
        final Date lastMonthDate = lastMonth.getTime();
        final Date thisYearDate = thisYear.getTime();

        for (final MessageInfo<T> message : messages)
        {
            final Date date = message.getDate();
            if (!todayDate.after(date))
            {
                todayList.add(message);
            }
            else if (!yesterdayDate.after(date))
            {
                yesterdayList.add(message);
            }
            else if (!thisWeekDate.after(date))
            {
                thisWeekList.add(message);
            }
            else if (!lastWeekDate.after(date))
            {
                lastWeekList.add(message);
            }
            else if (!thisMonthDate.after(date))
            {
                thisMonthList.add(message);
            }
            else if (!lastMonthDate.after(date))
            {
                lastMonthList.add(message);
            }
            else if (!thisYearDate.after(date))
            {
                thisYearList.add(message);
            }
            else
            {
                earlierList.add(message);
            }
        }

        final ArrayList<MessageGroup<T>> groups = new ArrayList<MessageGroup<T>>(3);

        if (!todayList.isEmpty())
        {
            final SimpleMessageGroup<T> group = new SimpleMessageGroup<T>();
            group.setMessages(todayList);
            group.setSubject(mContext.getString(R.string.group_by_date_today));
            group.setId(0);
            groups.add(group);
        }
        if (!yesterdayList.isEmpty())
        {
            final SimpleMessageGroup<T> group = new SimpleMessageGroup<T>();
            group.setMessages(yesterdayList);
            group.setSubject(mContext.getString(R.string.group_by_date_yesterday));
            group.setId(1);
            groups.add(group);
        }
        if (!thisWeekList.isEmpty())
        {
            final SimpleMessageGroup<T> group = new SimpleMessageGroup<T>();
            group.setMessages(thisWeekList);
            group.setSubject(mContext.getString(R.string.group_by_date_this_week));
            group.setId(2);
            groups.add(group);
        }
        if (!lastWeekList.isEmpty())
        {
            final SimpleMessageGroup<T> group = new SimpleMessageGroup<T>();
            group.setMessages(lastWeekList);
            group.setSubject(mContext.getString(R.string.group_by_date_last_week));
            group.setId(3);
            groups.add(group);
        }
        if (!thisMonthList.isEmpty())
        {
            final SimpleMessageGroup<T> group = new SimpleMessageGroup<T>();
            group.setMessages(thisMonthList);
            group.setSubject(mContext.getString(R.string.group_by_date_this_month));
            group.setId(4);
            groups.add(group);
        }
        if (!lastMonthList.isEmpty())
        {
            final SimpleMessageGroup<T> group = new SimpleMessageGroup<T>();
            group.setMessages(lastMonthList);
            group.setSubject(mContext.getString(R.string.group_by_date_last_month));
            group.setId(5);
            groups.add(group);
        }
        if (!thisYearList.isEmpty())
        {
            final SimpleMessageGroup<T> group = new SimpleMessageGroup<T>();
            group.setMessages(thisYearList);
            group.setSubject(mContext.getString(R.string.group_by_date_this_year));
            group.setId(6);
            groups.add(group);
        }
        if (!earlierList.isEmpty())
        {
            final SimpleMessageGroup<T> group = new SimpleMessageGroup<T>();
            group.setMessages(earlierList);
            group.setSubject(mContext.getString(R.string.group_by_date_earlier));
            group.setId(7);
            groups.add(group);
        }

        return groups;
    }

}
