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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * <p>
 * The CharsetDecoder used to decode both variants of the UTF-7 charset and the
 * modified-UTF-7 charset.
 * </p>
 * 
 * @author Jaap Beetstra
 */
class UTF7StyleCharsetDecoder extends CharsetDecoder {
    private final Base64Util base64;
    private final byte shift;
    private final byte unshift;
    private final boolean strict;
    private boolean base64mode;
    private int bitsRead;
    private int tempChar;
    private boolean justShifted;
    private boolean justUnshifted;

    UTF7StyleCharsetDecoder(UTF7StyleCharset cs, Base64Util base64, boolean strict) {
        super(cs, 0.6f, 1.0f);
        this.base64 = base64;
        this.strict = strict;
        this.shift = cs.shift();
        this.unshift = cs.unshift();
    }

    /*
     * (non-Javadoc)
     * @see java.nio.charset.CharsetDecoder#decodeLoop(java.nio.ByteBuffer,
     * java.nio.CharBuffer)
     */
    protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
        while (in.hasRemaining()) {
            byte b = in.get();
            if (base64mode) {
                if (b == unshift) {
                    if (base64bitsWaiting())
                        return malformed(in);
                    if (justShifted) {
                        if (!out.hasRemaining())
                            return overflow(in);
                        out.put((char)shift);
                    } else
                        justUnshifted = true;
                    setUnshifted();
                } else {
                    if (!out.hasRemaining())
                        return overflow(in);
                    CoderResult result = handleBase64(in, out, b);
                    if (result != null)
                        return result;
                }
                justShifted = false;
            } else {
                if (b == shift) {
                    base64mode = true;
                    if (justUnshifted && strict)
                        return malformed(in);
                    justShifted = true;
                    continue;
                }
                if (!out.hasRemaining())
                    return overflow(in);
                out.put((char)b);
                justUnshifted = false;
            }
        }
        return CoderResult.UNDERFLOW;
    }

    private CoderResult overflow(ByteBuffer in) {
        in.position(in.position() - 1);
        return CoderResult.OVERFLOW;
    }

    /**
     * <p>
     * Decodes a byte in <i>base 64 mode</i>. Will directly write a character to
     * the output buffer if completed.
     * </p>
     * 
     * @param in The input buffer
     * @param out The output buffer
     * @param lastRead Last byte read from the input buffer
     * @return CoderResult.malformed if a non-base 64 character was encountered
     *         in strict mode, null otherwise
     */
    private CoderResult handleBase64(ByteBuffer in, CharBuffer out, byte lastRead) {
        CoderResult result = null;
        int sextet = base64.getSextet(lastRead);
        if (sextet >= 0) {
            bitsRead += 6;
            if (bitsRead < 16) {
                tempChar += sextet << (16 - bitsRead);
            } else {
                bitsRead -= 16;
                tempChar += sextet >> (bitsRead);
                out.put((char)tempChar);
                tempChar = (sextet << (16 - bitsRead)) & 0xFFFF;
            }
        } else {
            if (strict)
                return malformed(in);
            out.put((char)lastRead);
            if (base64bitsWaiting())
                result = malformed(in);
            setUnshifted();
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.nio.charset.CharsetDecoder#implFlush(java.nio.CharBuffer)
     */
    protected CoderResult implFlush(CharBuffer out) {
        if ((base64mode && strict) || base64bitsWaiting())
            return CoderResult.malformedForLength(1);
        return CoderResult.UNDERFLOW;
    }

    /*
     * (non-Javadoc)
     * @see java.nio.charset.CharsetDecoder#implReset()
     */
    protected void implReset() {
        setUnshifted();
        justUnshifted = false;
    }

    /**
     * <p>
     * Resets the input buffer position to just before the last byte read, and
     * returns a result indicating to skip the last byte.
     * </p>
     * 
     * @param in The input buffer
     * @return CoderResult.malformedForLength(1);
     */
    private CoderResult malformed(ByteBuffer in) {
        in.position(in.position() - 1);
        return CoderResult.malformedForLength(1);
    }

    /**
     * @return True if there are base64 encoded characters waiting to be written
     */
    private boolean base64bitsWaiting() {
        return tempChar != 0 || bitsRead >= 6;
    }

    /**
     * <p>
     * Updates internal state to reflect the decoder is no longer in <i>base 64
     * mode</i>
     * </p>
     */
    private void setUnshifted() {
        base64mode = false;
        bitsRead = 0;
        tempChar = 0;
    }
}
