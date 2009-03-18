package com.android.email.mail.internet.wbxml.activesync;

import com.android.email.mail.internet.wbxml.CodePage;

/**
 * This class is the specific code page for Ping in the ActiveSync protocol.
 * The code page number is 13.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
public class PingCodePage extends CodePage {
    /**
     * Constructor for PingCodePage.  Initializes all of the code page values.
     */
    public PingCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("Ping", 0x05);
        codepageTokens.put("AutdState", 0x06); /* Not used by protocol */
        codepageTokens.put("Status", 0x07);
        codepageTokens.put("HeartbeatInterval", 0x08);
        codepageTokens.put("Folders", 0x09);
        codepageTokens.put("Folder", 0x0a);
        codepageTokens.put("Id", 0x0b);
        codepageTokens.put("Class", 0x0c);
        codepageTokens.put("MaxFolders", 0x0d);
         
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 0x0d;
        codePageName = "Ping";
    }
}
