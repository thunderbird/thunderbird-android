/* Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. */

package com.fsck.k9.mail.store.exchange.adapter;


/** contains the WBXML constants  */


public interface Wbxml {

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
