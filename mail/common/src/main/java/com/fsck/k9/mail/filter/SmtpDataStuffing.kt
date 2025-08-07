package com.fsck.k9.mail.filter

import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream

class SmtpDataStuffing(out: OutputStream?) : FilterOutputStream(out) {
    private var state: Int = STATE_CRLF

    @Throws(IOException::class)
    override fun write(oneByte: Int) {
        if (oneByte == '\r'.code) {
            state = STATE_CR
        } else if ((state == STATE_CR) && (oneByte == '\n'.code)) {
            state = STATE_CRLF
        } else if ((state == STATE_CRLF) && (oneByte == '.'.code)) {
            // Read <CR><LF><DOT> so this line needs an additional period.
            super.write('.'.code)
            state = STATE_NORMAL
        } else {
            state = STATE_NORMAL
        }
        super.write(oneByte)
    }

    companion object {
        private const val STATE_NORMAL = 0
        private const val STATE_CR = 1
        private const val STATE_CRLF = 2
    }
}
