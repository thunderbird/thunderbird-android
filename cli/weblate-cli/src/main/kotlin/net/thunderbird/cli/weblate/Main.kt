package net.thunderbird.cli.weblate

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import net.thunderbird.cli.weblate.command.CreateComponent
import net.thunderbird.cli.weblate.command.DeleteComponent
import net.thunderbird.cli.weblate.command.UpdateComponent

fun main(args: Array<String>) = WeblateCli()
    .subcommands(
        UpdateComponent(),
        CreateComponent(),
        DeleteComponent(),
    ).main(args)
