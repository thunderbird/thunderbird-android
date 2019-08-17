package com.fsck.k9.backend.eas;

public interface WbXml {

    // Shift applied to page numbers to generate tag
    public static final int PAGE_SHIFT = 6;
    public static final int PAGE_MASK = 0x3F;  // 6 bits
    public static final int CONTENT_MASK = 0x40;

    static public final int SWITCH_PAGE = 0;
    static public final int END = 1;
    static public final int ENTITY = 2;
    static public final int STR_I = 3;
    static public final int LITERAL = 4;
    static public final int EXT_I_0 = 0x40;
    static public final int EXT_I_1 = 0x41;
    static public final int EXT_I_2 = 0x42;
    static public final int PI = 0x43;
    static public final int LITERAL_C = 0x44;
    static public final int EXT_T_0 = 0x80;
    static public final int EXT_T_1 = 0x81;
    static public final int EXT_T_2 = 0x82;
    static public final int STR_T = 0x83;
    static public final int LITERAL_A = 0x084;
    static public final int EXT_0 = 0x0c0;
    static public final int EXT_1 = 0x0c1;
    static public final int EXT_2 = 0x0c2;
    static public final int OPAQUE = 0x0c3;
    static public final int LITERAL_AC = 0x0c4;
}
