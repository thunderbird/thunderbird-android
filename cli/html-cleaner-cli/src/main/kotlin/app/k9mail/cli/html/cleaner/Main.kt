package app.k9mail.cli.html.cleaner

import app.k9mail.html.cleaner.HtmlHeadProvider
import app.k9mail.html.cleaner.HtmlProcessor
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.inputStream
import java.io.File
import okio.buffer
import okio.sink
import okio.source

@Suppress("MemberVisibilityCanBePrivate")
class HtmlCleaner : CliktCommand() {
    val input by argument(help = "HTML input file (needs to be UTF-8 encoded)")
        .inputStream()

    val output by argument(help = "Output file")
        .file(mustExist = false, canBeDir = false)
        .optional()

    override fun help(context: Context) =
        "A tool that modifies HTML to only keep allowed elements and attributes the same way that K-9 Mail does."

    override fun run() {
        val html = readInput()
        val processedHtml = cleanHtml(html)
        writeOutput(processedHtml)
    }

    private fun readInput(): String {
        return input.source().buffer().use { it.readUtf8() }
    }

    private fun cleanHtml(html: String): String {
        val htmlProcessor = HtmlProcessor(
            object : HtmlHeadProvider {
                override val headHtml = """<meta name="viewport" content="width=device-width"/>"""
            },
        )

        return htmlProcessor.processForDisplay(html)
    }

    private fun writeOutput(data: String) {
        output?.writeOutput(data) ?: echo(data)
    }

    private fun File.writeOutput(data: String) {
        sink().buffer().use {
            it.writeUtf8(data)
        }
    }
}

fun main(args: Array<String>) = HtmlCleaner().main(args)
