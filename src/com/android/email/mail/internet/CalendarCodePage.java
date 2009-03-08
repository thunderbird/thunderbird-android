package com.android.email;

/**
 * This class is the specific code page for Calendar in the ActiveSync protocol.
 * The code page number is 4.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
class CalendarCodePage extends CodePage {
    /**
     * Constructor for CalendarCodePage.  Initializes all of the code page values.
     */
    public CalendarCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("TimeZone", 0x05);
        codepageTokens.put("AllDayEvent", 0x06);
        codepageTokens.put("Attendees", 0x07);
        codepageTokens.put("Attendee", 0x08);
        codepageTokens.put("Attendee_Email", 0x09);
        codepageTokens.put("Attendee_Name", 0x0a);
        codepageTokens.put("Body", 0x0b);
        codepageTokens.put("BodyTruncated", 0x0c);
        codepageTokens.put("BusyStatus", 0x0d);
        codepageTokens.put("Categories", 0x0e);
        codepageTokens.put("Category", 0x0f);
        codepageTokens.put("Compressed_RTF", 0x10);
        codepageTokens.put("DTStamp", 0x11);
        codepageTokens.put("EndTime", 0x12);
        codepageTokens.put("Exception", 0x13);
        codepageTokens.put("Exceptions", 0x14);
        codepageTokens.put("Exception_IsDeleted", 0x15);
        codepageTokens.put("Exception_StartTime", 0x16);
        codepageTokens.put("Location", 0x17);
        codepageTokens.put("MeetingStatus", 0x18);
        codepageTokens.put("Organizer_Email", 0x19);
        codepageTokens.put("Organizer_Name", 0x1a);
        codepageTokens.put("Recurrence", 0x1b);
        codepageTokens.put("Recurrence_Type", 0x1c);
        codepageTokens.put("Recurrence_Until", 0x1d);
        codepageTokens.put("Recurrence_Occurrences", 0x1e);
        codepageTokens.put("Recurrence_Interval", 0x1f);
        codepageTokens.put("Recurrence_DayOfWeek", 0x20);
        codepageTokens.put("Recurrence_DayOfMonth", 0x21);
        codepageTokens.put("Recurrence_WeekOfMonth", 0x22);
        codepageTokens.put("Recurrence_MonthOfYear", 0x23);
        codepageTokens.put("Reminder_MinsBefore", 0x24);
        codepageTokens.put("Sensitivity", 0x25);
        codepageTokens.put("Subject", 0x26);
        codepageTokens.put("StartTime", 0x27);
        codepageTokens.put("UID", 0x28);
        codepageTokens.put("Attendee_Status", 0x29);
        codepageTokens.put("Attendee_Type", 0x2a);
        
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 0x03;
        codePageName = "Calendar";
    }
}
