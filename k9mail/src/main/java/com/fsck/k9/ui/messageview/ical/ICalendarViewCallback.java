package com.fsck.k9.ui.messageview.ical;


import com.fsck.k9.mailstore.ICalendarViewInfo;

public interface ICalendarViewCallback {
    void onViewICalendar(ICalendarViewInfo attachment);
    void onSaveICalendar(ICalendarViewInfo attachment);
    void onSaveICalendarToUserProvidedDirectory(ICalendarViewInfo attachment);
}
