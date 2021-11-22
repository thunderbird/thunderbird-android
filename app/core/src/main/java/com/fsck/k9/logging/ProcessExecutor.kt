package com.fsck.k9.logging

import java.io.InputStream

interface ProcessExecutor {
    fun exec(command: String): InputStream
}

class RealProcessExecutor : ProcessExecutor {
    override fun exec(command: String): InputStream {
        val process = Runtime.getRuntime().exec(command)
        return process.inputStream
    }
}
