package net.thunderbird.core.logging.internal

import java.io.InputStream

internal interface ProcessExecutor {
    fun exec(command: String): InputStream
}

internal class RealProcessExecutor : ProcessExecutor {
    override fun exec(command: String): InputStream {
        val process = Runtime.getRuntime().exec(command)
        return process.inputStream
    }
}
