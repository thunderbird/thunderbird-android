package com.android.email.mail.internet;

import com.android.email.mail.internet.CodePage;

/**
 * This class is the specific code page for Emails in the ActiveSync protocol.
 * The code page number is 2.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
public class EmailCodePage extends CodePage {
    /**
     * Constructor for EmailCodePage.  Initializes all of the code page values.
     */
    public EmailCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("Attachment", 0x05);
        codepageTokens.put("Attachments", 0x06);
        codepageTokens.put("AttName", 0x07);
        codepageTokens.put("AttSize", 0x08);
        codepageTokens.put("Att0Id", 0x09);
        codepageTokens.put("AttMethod", 0x0a);
        codepageTokens.put("AttRemoved", 0x0b);
        codepageTokens.put("Body", 0x0c);
        codepageTokens.put("BodySize", 0x0d);
        codepageTokens.put("BodyTruncated", 0x0e);
        codepageTokens.put("DateReceived", 0x0f);
        codepageTokens.put("DisplayName", 0x10);
        codepageTokens.put("DisplayTo", 0x11);
        codepageTokens.put("Importance", 0x12);
        codepageTokens.put("MessageClass", 0x13);
        codepageTokens.put("Subject", 0x14);
        codepageTokens.put("Read", 0x15);
        codepageTokens.put("To", 0x16);
        codepageTokens.put("CC", 0x17);
        codepageTokens.put("From", 0x18);
        codepageTokens.put("ReplyTo", 0x19);
        codepageTokens.put("AllDayEvent", 0x1a);
        codepageTokens.put("Categories", 0x1b);
        codepageTokens.put("Category", 0x1c);
        codepageTokens.put("DTStamp", 0x1d);
        codepageTokens.put("EndTime", 0x1e);
        codepageTokens.put("InstanceType", 0x1f);
        codepageTokens.put("IntDBusyStatus", 0x20);
        codepageTokens.put("Location", 0x21);
        codepageTokens.put("MeetingRequest", 0x22);
        codepageTokens.put("Organizer", 0x23);
        codepageTokens.put("RecurrenceId", 0x24);
        codepageTokens.put("Reminder", 0x25);
        codepageTokens.put("ResponseRequested", 0x26);
        codepageTokens.put("Recurrences", 0x27);
        codepageTokens.put("Recurrence", 0x28);
        codepageTokens.put("Recurrence_Type", 0x29);
        codepageTokens.put("Recurrence_Until", 0x2a);
        codepageTokens.put("Recurrence_Occurrences", 0x2b);
        codepageTokens.put("Recurrence_Interval", 0x2c);
        codepageTokens.put("Recurrence_DayOfWeek", 0x2d);
        codepageTokens.put("Recurrence_DayOfMonth", 0x2e);
        codepageTokens.put("Recurrence_WeekOfMonth", 0x2f);
        codepageTokens.put("Recurrence_MonthOfYear", 0x30);
        codepageTokens.put("StartTime", 0x31);
        codepageTokens.put("Sensitivity", 0x32);
        codepageTokens.put("TimeZone", 0x33);
        codepageTokens.put("GlobalObjId", 0x34);
        codepageTokens.put("ThreadTopic", 0x35);
        codepageTokens.put("MIMEData", 0x36);
        codepageTokens.put("MIMETruncated", 0x37);
        codepageTokens.put("MIMESize", 0x38);
        codepageTokens.put("InternetCPID", 0x39);
        codepageTokens.put("Flag", 0x3a);
        codepageTokens.put("FlagStatus", 0x3b);
        codepageTokens.put("ContentClass", 0x3c);
        codepageTokens.put("FlagType", 0x3d);
        codepageTokens.put("CompleteTime", 0x3e);

        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 0x02;
        codePageName = "Email";
    }
}
