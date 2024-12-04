package net.thunderbird.cli.resource.mover

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split

class ResourceMoverCli(
    private val stringResourceMover: StringResourceMover = StringResourceMover(),
) : CliktCommand(
    name = "resource-mover",
) {
    private val from: String by option(
        help = "Source module path",
    ).required()

    private val to: String by option(
        help = "Target module path",
    ).required()

    private val keys: List<String> by option(
        help = "Keys to move",
    ).split(",").required()

    override fun help(context: Context): String = "Move string resources from one file to another"

    override fun run() {
        stringResourceMover.moveKeys(from, to, keys)
    }
}
