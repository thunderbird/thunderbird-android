package com.android.email.mail.internet;

import com.android.email.mail.internet.CodePage;

/**
 * This class is the specific code page for AirNotify in the ActiveSync protocol.
 * The code page number is 3.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
public class AirNotifyCodePage extends CodePage {
    /**
     * Constructor for AirNotifyCodePage.  Initializes all of the code page values.
     */
    public AirNotifyCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("Notify", 0x05);
        codepageTokens.put("Notification", 0x06);
        codepageTokens.put("Version", 0x07);
        codepageTokens.put("LifeTime", 0x08);
        codepageTokens.put("DeviceInfo", 0x09);
        codepageTokens.put("Enable", 0x0a);
        codepageTokens.put("Folder", 0x0b);
        codepageTokens.put("ServerId", 0x0c);
        codepageTokens.put("DeviceAddress", 0x0d);
        codepageTokens.put("ValidCarrierProfiles", 0x0e);
        codepageTokens.put("CarrierProfile", 0x0f);
        codepageTokens.put("Status", 0x10);
        codepageTokens.put("Responses", 0x11);
        codepageTokens.put("Devices", 0x12);
        codepageTokens.put("Device", 0x13);
        codepageTokens.put("Id", 0x14);
        codepageTokens.put("Expiry", 0x15);
        codepageTokens.put("NotifyGUID", 0x16);
        codepageTokens.put("DeviceFriendlyName", 0x17);
        
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        attributeTokens.put("Version=\"1.1\"", 0x05);
        attributeStrings.put(0x05, "Version=\"1.1\"");
        
        codePageIndex = 0x02;
        codePageName = "AirNotify";
    }
}
