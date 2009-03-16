package com.android.email.mail.internet;

import com.android.email.mail.internet.CodePage;

/**
 * This class is the specific code page for Contacts2 in the ActiveSync protocol.
 * The code page number is 12.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
public class Contacts2CodePage extends CodePage {
    /**
     * Constructor for Contacts2CodePage.  Initializes all of the code page values.
     */
    public Contacts2CodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("CustomerId", 0x05);
        codepageTokens.put("GovernmentId", 0x06);
        codepageTokens.put("IMAddress", 0x07);
        codepageTokens.put("IMAddress2", 0x08);
        codepageTokens.put("IMAddress3", 0x09);
        codepageTokens.put("ManagerName", 0x0a);
        codepageTokens.put("CompanyMainPhone", 0x0b);
        codepageTokens.put("AccountName", 0x0c);
        codepageTokens.put("NickName", 0x0d);
        codepageTokens.put("MMS", 0x0e);
         
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 0x0c;
        codePageName = "Contacts2";
    }
}
