package com.android.email;

/**
 * This class is the specific code page for FolderHierarchy in the ActiveSync protocol.
 * The code page number is 7.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
class FolderHierarchyCodePage extends CodePage {
    /**
     * Constructor for FolderHierarchyCodePage.  Initializes all of the code page values.
     */
    public FolderHierarchyCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("Folders", 0x05);
        codepageTokens.put("Folder", 0x06);
        codepageTokens.put("DisplayName", 0x07);
        codepageTokens.put("ServerId", 0x08);
        codepageTokens.put("ParentId", 0x09);
        codepageTokens.put("Type", 0x0a);
        codepageTokens.put("Response", 0x0b);
        codepageTokens.put("Status", 0x0c);
        codepageTokens.put("ContentClass", 0x0d);
        codepageTokens.put("Changes", 0x0e);
        codepageTokens.put("Add", 0x0f);
        codepageTokens.put("Delete", 0x10);
        codepageTokens.put("Update", 0x11);
        codepageTokens.put("SyncKey", 0x12);
        codepageTokens.put("FolderCreate", 0x13);
        codepageTokens.put("FolderDelete", 0x14);
        codepageTokens.put("FolderUpdate", 0x15);
        codepageTokens.put("FolderSync", 0x16);
        codepageTokens.put("Count", 0x17);
        codepageTokens.put("Version", 0x18);
        
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 0x07;
        codePageName = "FolderHierarchy";
    }
}
