package com.android.email.mail.internet.wbxml.activesync;

import com.android.email.mail.internet.wbxml.CodePage;

/**
 * This class is the specific code page for Settings in the ActiveSync protocol.
 * The code page number is 18.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
public class SettingsCodePage extends CodePage {
    /**
     * Constructor for SettingsCodePage.  Initializes all of the code page values.
     */
    public SettingsCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("Settings", 0x05);
        codepageTokens.put("Status", 0x06);
        codepageTokens.put("Get", 0x07);
        codepageTokens.put("Set", 0x08);
        codepageTokens.put("Oof", 0x09);
        codepageTokens.put("OofState", 0x0a);
        codepageTokens.put("StartTime", 0x0b);
        codepageTokens.put("EndTime", 0x0c);
        codepageTokens.put("OofMessage", 0x0d);
        codepageTokens.put("AppliesToInternal", 0x0e);
        codepageTokens.put("AppliesToExternalKnown", 0x0f);
        codepageTokens.put("AppliesToExternalUnknown", 0x10);
        codepageTokens.put("Enabled", 0x11);
        codepageTokens.put("ReplyMessage", 0x12);
        codepageTokens.put("BodyType", 0x13);
        codepageTokens.put("DevicePassword", 0x14);
        codepageTokens.put("Password", 0x15);
        codepageTokens.put("DeviceInformation", 0x16);
        codepageTokens.put("Model", 0x17);
        codepageTokens.put("IMEI", 0x18);
        codepageTokens.put("FriendlyName", 0x19);
        codepageTokens.put("OS", 0x1a);
        codepageTokens.put("OSLanguage", 0x1b);
        codepageTokens.put("PhoneNumber", 0x1c);
        codepageTokens.put("UserInformation", 0x1d);
        codepageTokens.put("EmailAddresses", 0x1e);
        codepageTokens.put("SmtpAddress", 0x1f);
         
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 0x12;
        codePageName = "Settings";
    }
}
