package net.thunderbird.cli.weblate

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import net.thunderbird.cli.weblate.command.UpdateComponent

fun main(args: Array<String>) = WeblateCli().subcommands(UpdateComponent()).main(args)
