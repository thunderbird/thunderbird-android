package com.android.email.mail.internet.wbxml.activesync;

import com.android.email.mail.internet.wbxml.CodePage;

/**
 * This class is the specific code page for Move in the ActiveSync protocol.
 * The code page number is 5.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
public class MoveCodePage extends CodePage {
    /**
     * Constructor for MoveCodePage.  Initializes all of the code page values.
     */
    public MoveCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("MoveItems", 0x05);
        codepageTokens.put("Move", 0x06);
        codepageTokens.put("SrcMsgId", 0x07);
        codepageTokens.put("SrcFldId", 0x08);
        codepageTokens.put("DstFldId", 0x09);
        codepageTokens.put("Response", 0x0a);
        codepageTokens.put("Status", 0x0b);
        codepageTokens.put("DstMsgId", 0x0c);
        
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 0x05;
        codePageName = "Move";
    }
}
