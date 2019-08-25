package com.fsck.k9.backend.eas;

public class WbXml {

    // Shift applied to page numbers to generate tag
    public static final int PAGE_SHIFT = 6;
    public static final int PAGE_MASK = 0x3F;  // 6 bits
    public static final int CONTENT_MASK = 0x40;

    public static final int SWITCH_PAGE = 0;
    public static final int END = 1;
    public static final int ENTITY = 2;
    public static final int STR_I = 3;
}
