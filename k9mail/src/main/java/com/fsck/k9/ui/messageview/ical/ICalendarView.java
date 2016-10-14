package com.fsck.k9.ui.messageview.ical;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.fsck.k9.ical.ICalData.ICalendarData;
import com.fsck.k9.mailstore.ICalendarViewInfo;

public abstract class ICalendarView extends LinearLayout {

    public ICalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    abstract public void setCallback(ICalendarViewCallback iCalendarCallback);

    abstract public void setICalendar(ICalendarViewInfo viewInfo, ICalendarData iCalendar);

    abstract public void setShowSummary(boolean shouldShowSummary);
}
