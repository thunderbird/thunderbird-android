package com.android.email.mail.internet;

import com.android.email.mail.internet.CodePage;

/**
 * This class is the specific code page for DocumentLibrary in the ActiveSync protocol.
 * The code page number is 19.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
public class DocumentLibraryCodePage extends CodePage {
    /**
     * Constructor for DocumentLibraryCodePage.  Initializes all of the code page values.
     */
    public DocumentLibraryCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("LinkId", 0x05);
        codepageTokens.put("DisplayName", 0x06);
        codepageTokens.put("IsFolder", 0x07);
        codepageTokens.put("CreationDate", 0x08);
        codepageTokens.put("LastModifiedDate", 0x09);
        codepageTokens.put("IsHidden", 0x0a);
        codepageTokens.put("ContentLength", 0x0b);
        codepageTokens.put("ContentType", 0x0c);
         
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 0x13;
        codePageName = "DocumentLibrary";
    }
}
