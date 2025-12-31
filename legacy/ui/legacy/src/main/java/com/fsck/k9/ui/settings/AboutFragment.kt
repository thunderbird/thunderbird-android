package com.fsck.k9.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import com.fsck.k9.ui.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject
import app.k9mail.core.ui.legacy.designsystem.R as DesignSystemR
import app.k9mail.core.ui.legacy.theme2.common.R as Theme2CommonR

class AboutFragment : Fragment() {
    private val appNameProvider: AppNameProvider by inject()
    private val themeProvider: FeatureThemeProvider by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
            )
            setContent {
                themeProvider.WithTheme {
                    AboutScreen(
                        appName = appNameProvider.appName,
                        versionNumber = getVersionNumber() ?: "?",
                        displayChangeLog = { findNavController().navigate(R.id.action_aboutScreen_to_changelogScreen) },
                        displayAuthors = { openUrl(getString(R.string.app_authors_url)) },
                        displayLicense = { openUrl(getString(R.string.app_license_url)) },
                        displayWebSite = { openUrl(getString(R.string.app_webpage_url)) },
                        displayForum = { openUrl(getString(R.string.user_forum_url)) },
                    )
                }
            }
        }
    }

    private fun getVersionNumber(): String? {
        return try {
            val context = requireContext()
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(e, "Error getting PackageInfo")
            null
        }
    }
}

private fun Fragment.openUrl(url: String) = requireContext().openUrl(url)

private fun Context.openUrl(url: String) {
    try {
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(viewIntent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, R.string.error_activity_not_found, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun LibraryList(
    libraries: ImmutableList<Library>,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        libraries.forEach { library ->
            LibraryItem(library = library)
        }
    }
}

@Composable
fun LibraryItem(
    library: Library,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .selectable(
                selected = false,
                onClick = { context.openUrl(library.url) },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        TextTitleMedium(
            text = library.name,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            color = MainTheme.colors.secondary,
        )
        TextBodyMedium(
            text = library.license,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            color = MainTheme.colors.secondary,
        )
    }
}

@Composable
fun AboutScreen(
    appName: String,
    versionNumber: String,
    modifier: Modifier = Modifier,
    displayChangeLog: () -> Unit = {},
    displayAuthors: () -> Unit = {},
    displayLicense: () -> Unit = {},
    displayWebSite: () -> Unit = {},
    displayForum: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            AppLogo()
            SectionTitle(title = context.getString(R.string.about_title, appName))
            SectionContent(
                sectionLabel = context.getString(R.string.version),
                sectionText = versionNumber,
                sectionImageId = DesignSystemR.drawable.ic_info,
                onClick = displayChangeLog,
            )
            SectionContent(
                sectionLabel = context.getString(R.string.authors),
                sectionText = context.getString(R.string.about_app_authors_k9),
                secondarySectionText = context.getString(R.string.about_app_authors_thunderbird),
                sectionImageId = DesignSystemR.drawable.ic_group,
                onClick = displayAuthors,
            )

            SectionContent(
                sectionLabel = context.getString(R.string.license),
                sectionText = context.getString(R.string.app_license),
                sectionImageId = DesignSystemR.drawable.ic_code,
                onClick = displayLicense,
            )

            SectionTitle(title = context.getString(R.string.about_project_title))

            SectionContent(
                sectionLabel = context.getString(R.string.about_website_title),
                sectionText = context.getString(R.string.app_webpage_url),
                sectionImageId = DesignSystemR.drawable.ic_link,
                onClick = displayWebSite,
            )

            SectionContent(
                sectionLabel = context.getString(R.string.user_forum_title),
                sectionText = context.getString(R.string.user_forum_url),
                sectionImageId = DesignSystemR.drawable.ic_forum,
                onClick = displayForum,
            )

            SectionTitle(title = context.getString(R.string.about_libraries))
            LibraryList(libraries = USED_LIBRARIES)
        }
    }
}

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val typedValue = remember { TypedValue() }
    context.theme.resolveAttribute(Theme2CommonR.attr.appLogo, typedValue, true)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = typedValue.resourceId),
            modifier = Modifier.size(size = 100.dp),
            contentDescription = null,
        )
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    TextTitleSmall(
        text = title,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 8.dp,
            ),
        color = MainTheme.colors.secondary,
    )
}

@Composable
fun SectionContent(
    sectionLabel: String,
    sectionText: String,
    sectionImageId: Int,
    modifier: Modifier = Modifier,
    secondarySectionText: String? = null,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = sectionImageId),
            modifier = Modifier
                .width(24.dp)
                .height(24.dp),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(30.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .wrapContentHeight(),
        ) {
            TextTitleMedium(
                text = sectionLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                color = MainTheme.colors.secondary,
            )

            TextBodyMedium(
                text = sectionText,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                color = MainTheme.colors.secondary,
            )

            secondarySectionText?.let {
                TextBodyMedium(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }
}

@Stable
data class Library(val name: String, val url: String, val license: String)

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
