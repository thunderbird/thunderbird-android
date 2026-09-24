package net.thunderbird.app.composition.core.file

import net.thunderbird.core.file.DefaultFileManager
import net.thunderbird.core.file.FileManager
import org.koin.dsl.module

internal val fileCompositionModule = module {
    single<FileManager> {
        DefaultFileManager(
            fileSystem = get(),
        )
    }
}
