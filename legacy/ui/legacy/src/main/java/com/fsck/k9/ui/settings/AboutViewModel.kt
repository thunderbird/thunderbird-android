package com.fsck.k9.ui.settings

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import com.fsck.k9.ui.settings.AboutContract.Effect
import com.fsck.k9.ui.settings.AboutContract.Event
import com.fsck.k9.ui.settings.AboutContract.State
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.common.provider.AppVersionProvider

internal class AboutViewModel(
    appVersionProvider: AppVersionProvider,
) : BaseViewModel<State, Event, Effect>(
    initialState = State(
        version = appVersionProvider.getVersionNumber(),
        libraries = USED_LIBRARIES,
    ),
) {
    override fun event(event: Event) {
        when (event) {
            is Event.OnChangeLogClick -> emitEffect(Effect.OpenChangeLog)
            is Event.OnSectionContentClick -> emitEffect(Effect.OpenUrl(event.url))
            is Event.OnLibraryClick -> emitEffect(Effect.OpenUrl(event.library.url))
        }
    }
}

private val USED_LIBRARIES = persistentListOf(
    Library(
        "Android Jetpack libraries",
        "https://developer.android.com/jetpack",
        "Apache License, Version 2.0",
    ),
    Library(
        "AndroidX Preference extended",
        "https://github.com/takisoft/preferencex-android",
        "Apache License, Version 2.0",
    ),
    Library("AppAuth for Android", "https://github.com/openid/AppAuth-Android", "Apache License, Version 2.0"),
    Library("Apache HttpComponents", "https://hc.apache.org/", "Apache License, Version 2.0"),
    Library("AutoValue", "https://github.com/google/auto", "Apache License, Version 2.0"),
    Library("CircleImageView", "https://github.com/hdodenhof/CircleImageView", "Apache License, Version 2.0"),
    Library("ckChangeLog", "https://github.com/cketti/ckChangeLog", "Apache License, Version 2.0"),
    Library("Commons IO", "https://commons.apache.org/io/", "Apache License, Version 2.0"),
    Library("ColorPicker", "https://github.com/gregkorossy/ColorPicker", "Apache License, Version 2.0"),
    Library("DateTimePicker", "https://github.com/gregkorossy/DateTimePicker", "Apache License, Version 2.0"),
    Library("Error Prone annotations", "https://github.com/google/error-prone", "Apache License, Version 2.0"),
    Library("FlexboxLayout", "https://github.com/google/flexbox-layout", "Apache License, Version 2.0"),
    Library("FastAdapter", "https://github.com/mikepenz/FastAdapter", "Apache License, Version 2.0"),
    Library("Glide", "https://github.com/bumptech/glide", "BSD, part MIT and Apache 2.0"),
    Library("jsoup", "https://jsoup.org/", "MIT License"),
    Library("jutf7", "http://jutf7.sourceforge.net/", "MIT License"),
    Library("JZlib", "http://www.jcraft.com/jzlib/", "BSD-style License"),
    Library("jcip-annotations", "https://jcip.net/", "Public License"),
    Library(
        "Jetbrains Annotations for JVM-based languages",
        "https://github.com/JetBrains/java-annotations",
        "Apache License, Version 2.0",
    ),
    Library(
        "Jetbrains Compose Runtime",
        "https://github.com/JetBrains/compose-multiplatform-core",
        "Apache License, Version 2.0",
    ),
    Library("Koin", "https://insert-koin.io/", "Apache License, Version 2.0"),
    Library(
        "Kotlin Android Extensions Runtime",
        "https://github.com/JetBrains/kotlin/tree/master/plugins/android-extensions/android-extensions-runtime",
        "Apache License, Version 2.0",
    ),
    Library("Kotlin Parcelize Runtime", "https://github.com/JetBrains/kotlin", "Apache License, Version 2.0"),
    Library(
        "Kotlin Standard Library",
        "https://kotlinlang.org/api/latest/jvm/stdlib/",
        "Apache License, Version 2.0",
    ),
    Library(
        "KotlinX Coroutines",
        "https://github.com/Kotlin/kotlinx.coroutines",
        "Apache License, Version 2.0",
    ),
    Library("KotlinX DateTime", "https://github.com/Kotlin/kotlinx-datetime", "Apache License, Version 2.0"),
    Library(
        "KotlinX Immutable Collections",
        "https://github.com/Kotlin/kotlinx.collections.immutable",
        "Apache License, Version 2.0",
    ),
    Library(
        "KotlinX Serialization",
        "https://github.com/Kotlin/kotlinx.serialization",
        "Apache License, Version 2.0",
    ),
    Library(
        "ListenableFuture",
        "https://github.com/google/guava",
        "Apache License, Version 2.0",
    ),
    Library(
        "Material Components for Android",
        "https://github.com/material-components/material-components-android",
        "Apache License, Version 2.0",
    ),
    Library("Mime4j", "https://james.apache.org/mime4j/", "Apache License, Version 2.0"),
    Library("MiniDNS", "https://github.com/MiniDNS/minidns", "Multiple, Apache License, Version 2.0"),
    Library("Moshi", "https://github.com/square/moshi", "Apache License, Version 2.0"),
    Library("OkHttp", "https://github.com/square/okhttp", "Apache License, Version 2.0"),
    Library("Okio", "https://github.com/square/okio", "Apache License, Version 2.0"),
    Library(
        "SafeContentResolver",
        "https://github.com/cketti/SafeContentResolver",
        "Apache License, Version 2.0",
    ),
    Library("SearchPreference", "https://github.com/ByteHamster/SearchPreference", "MIT License"),
    Library("SLF4J", "https://www.slf4j.org/", "MIT License"),
    Library("Stately", "https://github.com/touchlab/Stately", "Apache License, Version 2.0"),
    Library("Timber", "https://github.com/JakeWharton/timber", "Apache License, Version 2.0"),
    Library(
        "TokenAutoComplete",
        "https://github.com/splitwise/TokenAutoComplete/",
        "Apache License, Version 2.0",
    ),
    Library("ZXing", "https://github.com/zxing/zxing", "Apache License, Version 2.0"),
)
