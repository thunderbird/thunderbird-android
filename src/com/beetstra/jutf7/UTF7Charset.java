/* ====================================================================
 * Copyright (c) 2006 J.T. Beetstra
 *
 * Permission is hereby granted, free of charge, to any person obtaining 
 * a copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including 
 * without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to 
 * permit persons to whom the Software is furnished to do so, subject to 
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * ====================================================================
 */

package com.beetstra.jutf7;

/**
 * <p>
 * The character set specified in RFC 2152. Two variants are supported using the
 * encodeOptional constructor flag
 * </p>
 * 
 * @see <a href="http://tools.ietf.org/html/rfc2152">RFC 2152< /a>
 * @author Jaap Beetstra
 */
class UTF7Charset extends UTF7StyleCharset {
    private static final String BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz" + "0123456789+/";
    private static final String SET_D = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'(),-./:?";
    private static final String SET_O = "!\"#$%&*;<=>@[]^_`{|}";
    private static final String RULE_3 = " \t\r\n";
    final String directlyEncoded;

    UTF7Charset(String name, String[] aliases, boolean includeOptional) {
        super(name, aliases, BASE64_ALPHABET, false);
        if (includeOptional)
            this.directlyEncoded = SET_D + SET_O + RULE_3;
        else
            this.directlyEncoded = SET_D + RULE_3;
    }

    /*
     * (non-Javadoc)
     * @see com.beetstra.jutf7.UTF7StyleCharset#canEncodeDirectly(char)
     */
    boolean canEncodeDirectly(char ch) {
        return directlyEncoded.indexOf(ch) >= 0;
    }

    /*
     * (non-Javadoc)
     * @see com.beetstra.jutf7.UTF7StyleCharset#shift()
     */
    byte shift() {
        return '+';
    }

    /*
     * (non-Javadoc)
     * @see com.beetstra.jutf7.UTF7StyleCharset#unshift()
     */
    byte unshift() {
        return '-';
    }
}
