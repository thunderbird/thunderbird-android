package net.thunderbird.cli.badging

import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator
import java.io.File
import kotlin.system.exitProcess
import net.thunderbird.cli.badging.AnsiColors.ANSI_BOLD
import net.thunderbird.cli.badging.AnsiColors.ANSI_GREEN
import net.thunderbird.cli.badging.AnsiColors.ANSI_RED
import net.thunderbird.cli.badging.AnsiColors.ANSI_RESET
import net.thunderbird.cli.badging.AnsiColors.ANSI_YELLOW
import net.thunderbird.core.logging.Logger

class BadgingValidator(
    private val logger: Logger,
) {
    fun validate(
        goldenBadgingFile: File,
        actualBadging: String,
    ) {
        if (!goldenBadgingFile.exists()) {
            logger.error {
                "Golden badging file does not exist at ${goldenBadgingFile.absolutePath}. " +
                    "Run with --update to create it."
            }
            exitProcess(1)
        }

        val golden = goldenBadgingFile.readText()
        if (golden == actualBadging) {
            printBadgingDiff(ANSI_YELLOW + "Badging matches the golden." + ANSI_RESET)
            return
        } else {
            val diff = createDiff(golden, actualBadging)
            printBadgingDiff(
                ANSI_RED +
                    "Generated badging is different from golden badging! If intended, run again with --update." +
                    ANSI_RESET,
            )
            println()
            printColoredDiff(diff)
            exitProcess(1)
        }
    }

    private fun printBadgingDiff(message: String) {
        println()
        println(ANSI_BOLD + "Badging diff:" + ANSI_RESET)
        println()
        println(message)
        println()
    }

    private fun printColoredDiff(diff: String) {
        diff.lines().forEach { line ->
            val color = when {
                line.startsWith("+") -> ANSI_GREEN
                line.startsWith("-") -> ANSI_RED
                else -> null
            }
            if (color != null) {
                println(color + line + ANSI_RESET)
            } else {
                println(line)
            }
        }
    }

    private fun createDiff(golden: String, actual: String): String {
        val generator: DiffRowGenerator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .mergeOriginalRevised(true)
            .inlineDiffByWord(true)
            .oldTag { _ -> "" }
            .newTag { _ -> "" }
            .build()

        return generator.generateDiffRows(
            golden.lines(),
            actual.lines(),
        ).withIndex()
            .filter { (_, row) -> row.tag != DiffRow.Tag.EQUAL }
            .joinToString("\n") { (index, row) ->
                when (row.tag) {
                    DiffRow.Tag.INSERT -> "+ [${index + 1}] ${row.newLine}"
                    DiffRow.Tag.DELETE -> "- [${index + 1}] ${row.oldLine}"
                    DiffRow.Tag.CHANGE -> "+ [${index + 1}] ${row.newLine}\n- [${index + 1}] ${row.oldLine}"
                    DiffRow.Tag.EQUAL -> "" // filtered out
                }
            }
    }
}
