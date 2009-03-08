package com.android.email;

/**
 * This class is the specific code page for ItemEstimate in the ActiveSync protocol.
 * The code page number is 6.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
class ItemEstimateCodePage extends CodePage {
    /**
     * Constructor for ItemEstimateCodePage.  Initializes all of the code page values.
     */
    public ItemEstimateCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("GetItemEstimate", 0x05);
        codepageTokens.put("Version", 0x06);
        codepageTokens.put("Collections", 0x07);
        codepageTokens.put("Collection", 0x08);
        codepageTokens.put("Class", 0x09);
        codepageTokens.put("CollectionId", 0x0a);
        codepageTokens.put("DateTime", 0x0b);
        codepageTokens.put("Estimate", 0x0c);
        codepageTokens.put("Response", 0x0d);
        codepageTokens.put("Status", 0x0e);
        
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 0x06;
        codePageName = "ItemEstimate";
    }
}
