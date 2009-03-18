package com.android.email.mail.internet.wbxml;

import java.util.HashMap;

/**
 * This class represents the base class for defined WBXML code pages.  It is designed as a
 * base class that contains the minimal information needed for a code page (the definition of
 * standard WBXML tokens).
 *
 * @version 1.0
 * @author  Matthew Brace
 */
public class CodePage {
    protected HashMap<String, Integer> codepageTokens = new HashMap<String, Integer>();
    protected HashMap<Integer, String> codepageStrings = new HashMap<Integer, String>();
    protected HashMap<String, Integer> attributeTokens = new HashMap<String, Integer>();
    protected HashMap<Integer, String> attributeStrings = new HashMap<Integer, String>();
    protected int codePageIndex = -1;
    protected String codePageName = "Base";
    private static final HashMap<String, Integer> wbxmlTokens = new HashMap<String, Integer>();
    static {
        wbxmlTokens.put("switch_page", 0x00);
        wbxmlTokens.put("end", 0x01);
        wbxmlTokens.put("entity", 0x02);
        wbxmlTokens.put("str_i", 0x03);
        wbxmlTokens.put("literal", 0x04);
        wbxmlTokens.put("ext_i_0", 0x40);
        wbxmlTokens.put("ext_i_1", 0x41);
        wbxmlTokens.put("ext_i_2", 0x42);
        wbxmlTokens.put("pi", 0x43);
        wbxmlTokens.put("literal_c", 0x44);
        wbxmlTokens.put("ext_t_0", 0x80);
        wbxmlTokens.put("ext_t_1", 0x81);
        wbxmlTokens.put("ext_t_2", 0x82);
        wbxmlTokens.put("str_t", 0x83);
        wbxmlTokens.put("literal_a", 0x84);
        wbxmlTokens.put("ext_0", 0xc0);
        wbxmlTokens.put("ext_1", 0xc1);
        wbxmlTokens.put("ext_2", 0xc2);
        wbxmlTokens.put("opaque", 0xc3);
        wbxmlTokens.put("literal_ac", 0xc4);
    }
    private static final HashMap<Integer, String> wbxmlStrings = new HashMap<Integer, String>();
    static {
        wbxmlStrings.put(0x00, "switch_page");
        wbxmlStrings.put(0x01, "end");
        wbxmlStrings.put(0x02, "entity");
        wbxmlStrings.put(0x03, "str_i");
        wbxmlStrings.put(0x04, "literal");
        wbxmlStrings.put(0x40, "ext_i_0");
        wbxmlStrings.put(0x41, "ext_i_1");
        wbxmlStrings.put(0x42, "ext_i_2");
        wbxmlStrings.put(0x43, "pi");
        wbxmlStrings.put(0x44, "literal_c");
        wbxmlStrings.put(0x80, "ext_t_0");
        wbxmlStrings.put(0x81, "ext_t_1");
        wbxmlStrings.put(0x82, "ext_t_2");
        wbxmlStrings.put(0x83, "str_t");
        wbxmlStrings.put(0x84, "literal_a");
        wbxmlStrings.put(0xc0, "ext_0");
        wbxmlStrings.put(0xc1, "ext_1");
        wbxmlStrings.put(0xc2, "ext_2");
        wbxmlStrings.put(0xc3, "opaque");
        wbxmlStrings.put(0xc4, "literal_ac");
    }

    /**
     * Return the integer value for the standard WBXML token that corresponds to the supplied string.
     *
     * @param identity   The string identity of the WBXML token to retrieve
     * @return           The integer value of the corresponding token or -1 if it cannot be found
     */
    public Integer getWbxmlToken(String identity) {
        Integer token = -1;
        
        if (wbxmlTokens.containsKey(identity)) {
            token = wbxmlTokens.get(identity);
        }

        return token;
    }

    public String getWbxmlString(Integer token) {
        String identity = new String();

        if (wbxmlStrings.containsKey(token)) {
            identity = wbxmlStrings.get(token);
        }

        return identity;
    }

    /**
     * Return the code page index for this code page.
     *
     * @return The integer value of the code page, -1 for the base code page
     */
    public Integer getCodePageIndex() {
        return codePageIndex;
    }

    /**
     * Return the string representation for the name of this code page.
     *
     * @return The namespace of this code page
     */
    public String getCodePageName() {
        return codePageName;
    }
    
    /**
     * Return the integer value for the code page token that corresponds to the supplied string.
     *
     * @param identity  The string identity of the code page token to retrieve
     * @return          The integer value of the corresponding token or -1 if it cannot be found
     */
    public Integer getCodePageToken(String identity) {
        Integer token = -1;

        if (codepageTokens.containsKey(identity)) {
            token = codepageTokens.get(identity);
        }

        return token;
    }

    /**
     * Return the string value for the code page that corresponds to the supplied token.
     *
     * @param token  The integer value of the token of the string to retrieve
     * @return       The string value for the supplied token or an empty string if it cannot be found
     */
    public String getCodePageString(Integer token) {
        String identity = new String();

        if (codepageStrings.containsKey(token)) {
            identity = codepageStrings.get(token);
        }

        return identity;
    }

    /**
     * Return the integer value for the attribute token that corresponds to the supplied string.
     *
     * @param identity  The string identity of the code page token to retrieve
     * @return          The integer value of the corresponding token or -1 if it cannot be found
     */
    public Integer getAttributeToken(String identity) {
        Integer token = -1;

        if (attributeTokens.containsKey(identity)) {
            token = attributeTokens.get(identity);
        }
        System.out.println("returning "+token+" for "+identity);

        return token;
    }

    /**
     * Return the string value for the attribute that corresponds to the supplied token.
     *
     * @param token  The integer value of the token of the string to retrieve
     * @return       The string value for the supplied token or an empty string if it cannot be found
     */
    public String getAttributeString(Integer token) {
        String identity = new String();

        if (attributeStrings.containsKey(token)) {
            identity = attributeStrings.get(token);
        }

        return identity;
    }
}
