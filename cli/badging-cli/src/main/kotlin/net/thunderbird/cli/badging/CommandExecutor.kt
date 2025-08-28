package net.thunderbird.cli.badging

import java.io.File
import net.thunderbird.core.logging.Logger

data class CommandResult(
    val output: String,
    val error: String,
    val exitCode: Int,
)

class CommandExecutor(
    private val logger: Logger,
) {

    fun exec(command: List<String>, workingDir: File): CommandResult {
        val cmdString = command.joinToString(" ")

        logger.info { "Executing (cwd=${workingDir.absolutePath}): $cmdString" }
        val process = ProcessBuilder(command)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        val exitCode = process.waitFor()
        if (exitCode == 0) {
            logger.info { "Finished: $cmdString" }
        } else {
            logger.error { "Command failed (exit=$exitCode): $cmdString" }
        }

        return CommandResult(output = "", error = "", exitCode = exitCode)
    }

    fun execAndCapture(cmd: List<String>, workDir: File): CommandResult {
        val cmdString = cmd.joinToString(" ")

        logger.info { "Executing (cwd=${workDir.absolutePath}): $cmdString" }
        val process = ProcessBuilder(cmd)
            .directory(workDir)
            .start()
        val output = process.inputStream.readBytes().toString(Charsets.UTF_8)
        val error = process.errorStream.readBytes().toString(Charsets.UTF_8)

        val exitCode = process.waitFor()
        if (exitCode == 0) {
            logger.info { "Finished: $cmdString" }
        } else {
            logger.error { "Command failed (exit=$exitCode): $cmdString" }
            if (error.isNotBlank()) logger.error { "Error message: ${error.trim()}" }
        }

        return CommandResult(output = output, error = error, exitCode = exitCode)
    }
}
