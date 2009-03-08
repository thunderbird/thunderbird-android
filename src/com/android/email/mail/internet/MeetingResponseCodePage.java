package com.android.email;

/**
 * This class is the specific code page for MeetingResponse in the ActiveSync protocol.
 * The code page number is 8.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
class MeetingResponseCodePage extends CodePage {
    /**
     * Constructor for MeetingResponseCodePage.  Initializes all of the code page values.
     */
    public MeetingResponseCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("CalId", 0x05);
        codepageTokens.put("CollectionId", 0x06);
        codepageTokens.put("MeetingResponse", 0x07);
        codepageTokens.put("ReqId", 0x08);
        codepageTokens.put("Request", 0x09);
        codepageTokens.put("Result", 0x0a);
        codepageTokens.put("Status", 0x0b);
        codepageTokens.put("UserResponse", 0x0c);
        codepageTokens.put("Version", 0x0d);
        
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 0x08;
        codePageName = "MeetingResponse";
    }
}
