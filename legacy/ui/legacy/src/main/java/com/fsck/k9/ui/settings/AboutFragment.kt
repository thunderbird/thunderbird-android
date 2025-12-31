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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject
import app.k9mail.core.ui.legacy.designsystem.R as DesignSystemR
import app.k9mail.core.ui.legacy.theme2.common.R as Theme2CommonR

@Suppress("TooManyFunctions")
class AboutFragment : Fragment() {
    private val appNameProvider: AppNameProvider by inject()
    private val themeProvider: FeatureThemeProvider by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAppLogo(view)

        setVersionImage(view)

        setAuthorsImage(view)

        setSourceCodeImage(view)

        setLicenceImage(view)

        setLinkImage(view)

        setForumImage(view)

        setAboutTitle(view)

        setVersionLabel(view)

        setVersionText(view)

        setAuthorsLabel(view)

        setAuthorOne(view)

        setAuthorTwo(view)

        setLicenseLabel(view)

        setLicenseText(view)

        setAboutProjectTitle(view)

        setWebsiteLabel(view)

        setWebsiteText(view)

        setForumLabel(view)

        setForumText(view)

        setLibrariesTitle(view)

        val versionLayout = view.findViewById<View>(R.id.versionLayout)
        versionLayout.setOnClickListener { displayChangeLog() }

        val authorsLayout = view.findViewById<View>(R.id.authorsLayout)
        authorsLayout.setOnClickListener {
            openUrl(getString(R.string.app_authors_url))
        }

        val licenseLayout = view.findViewById<View>(R.id.licenseLayout)
        licenseLayout.setOnClickListener {
            openUrl(getString(R.string.app_license_url))
        }

        val sourceCodeLayout = view.findViewById<View>(R.id.sourceCodeLayout)
        sourceCodeLayout.setOnClickListener {
            openUrl(getString(R.string.app_source_url))
        }

        val websiteLayout = view.findViewById<View>(R.id.websiteLayout)
        websiteLayout.setOnClickListener {
            openUrl(getString(R.string.app_webpage_url))
        }

        val userForumLayout = view.findViewById<View>(R.id.userForumLayout)
        userForumLayout.setOnClickListener {
            openUrl(getString(R.string.user_forum_url))
        }

        val manager = LinearLayoutManager(view.context)
        val librariesRecyclerView = view.findViewById<RecyclerView>(R.id.libraries)
        librariesRecyclerView.apply {
            layoutManager = manager
            adapter = LibrariesAdapter(USED_LIBRARIES)
            isNestedScrollingEnabled = false
            isFocusable = false
        }
    }

    private fun setForumImage(view: View) {
        val forumImage = view.findViewById<ComposeView>(R.id.forum_image)
        forumImage.setContent {
            Image(
                painter = painterResource(id = DesignSystemR.drawable.ic_forum),
                modifier = Modifier.size(size = 32.dp),
                contentDescription = null,
            )
        }
    }

    private fun setLinkImage(view: View) {
        val linkImage = view.findViewById<ComposeView>(R.id.link_image)
        linkImage.setContent {
            Image(
                painter = painterResource(id = DesignSystemR.drawable.ic_link),
                modifier = Modifier.size(size = 32.dp),
                contentDescription = null,
            )
        }
    }

    private fun setLicenceImage(view: View) {
        val licenceImage = view.findViewById<ComposeView>(R.id.licence_image)
        licenceImage.setContent {
            Image(
                painter = painterResource(id = DesignSystemR.drawable.ic_description),
                modifier = Modifier.size(size = 32.dp),
                contentDescription = null,
            )
        }
    }

    private fun setSourceCodeImage(view: View) {
        val sourceCodeImage = view.findViewById<ComposeView>(R.id.source_code_image)
        sourceCodeImage.setContent {
            Image(
                painter = painterResource(id = DesignSystemR.drawable.ic_code),
                modifier = Modifier.size(size = 32.dp),
                contentDescription = null,
            )
        }
    }

    private fun setAuthorsImage(view: View) {
        val authorsImage = view.findViewById<ComposeView>(R.id.authors_image)
        authorsImage.setContent {
            Image(
                painter = painterResource(id = DesignSystemR.drawable.ic_group),
                modifier = Modifier.size(size = 32.dp),
                contentDescription = null,
            )
        }
    }

    private fun setVersionImage(view: View) {
        val versionImage = view.findViewById<ComposeView>(R.id.version_image)
        versionImage.setContent {
            Image(
                painter = painterResource(id = DesignSystemR.drawable.ic_info),
                modifier = Modifier.size(size = 32.dp),
                contentDescription = null,
            )
        }
    }

    private fun setAppLogo(view: View) {
        val appLogo = view.findViewById<ComposeView>(R.id.app_logo)
        appLogo.setContent {
            val context = LocalContext.current
            val typedValue = remember { TypedValue() }
            context.theme.resolveAttribute(Theme2CommonR.attr.appLogo, typedValue, true)
            Image(
                painter = painterResource(id = typedValue.resourceId),
                modifier = Modifier.size(size = 128.dp),
                contentDescription = null,
            )
        }
    }

    private fun setAboutTitle(view: View) {
        val aboutTitle = view.findViewById<ComposeView>(R.id.about_title)
        aboutTitle.setContent {
            themeProvider.WithTheme {
                val context = LocalContext.current
                TextTitleSmall(
                    text = context.getString(R.string.about_title, appNameProvider.appName),
                    modifier = Modifier
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
        }
    }

    private fun setVersionLabel(view: View) {
        val versionLabel = view.findViewById<ComposeView>(R.id.versionLabel)
        versionLabel.setContent {
            themeProvider.WithTheme {
                val context = LocalContext.current
                TextTitleMedium(
                    text = context.getString(R.string.version),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }

    private fun setVersionText(view: View) {
        val versionLabel = view.findViewById<ComposeView>(R.id.version)
        versionLabel.setContent {
            themeProvider.WithTheme {
                TextBodyMedium(
                    text = getVersionNumber() ?: "?",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }

    private fun setAuthorsLabel(view: View) {
        val authorsLabel = view.findViewById<ComposeView>(R.id.authorsLabel)
        authorsLabel.setContent {
            val context = LocalContext.current
            themeProvider.WithTheme {
                TextTitleMedium(
                    text = context.getString(R.string.authors),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }

    private fun setAuthorOne(view: View) {
        val authorOne = view.findViewById<ComposeView>(R.id.authorOne)
        authorOne.setContent {
            val context = LocalContext.current
            themeProvider.WithTheme {
                TextBodyMedium(
                    text = context.getString(R.string.about_app_authors_k9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }

    private fun setAuthorTwo(view: View) {
        val authorTwo = view.findViewById<ComposeView>(R.id.authorTwo)
        authorTwo.setContent {
            val context = LocalContext.current
            themeProvider.WithTheme {
                TextBodyMedium(
                    text = context.getString(R.string.about_app_authors_thunderbird),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }

    private fun setLicenseLabel(view: View) {
        val licenseLabel = view.findViewById<ComposeView>(R.id.licenseLabel)
        licenseLabel.setContent {
            val context = LocalContext.current
            themeProvider.WithTheme {
                TextTitleMedium(
                    text = context.getString(R.string.license),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }

    private fun setLicenseText(view: View) {
        val license = view.findViewById<ComposeView>(R.id.license)
        license.setContent {
            val context = LocalContext.current
            themeProvider.WithTheme {
                TextBodyMedium(
                    text = context.getString(R.string.app_license),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }

    private fun setAboutProjectTitle(view: View) {
        val aboutProjectTitle = view.findViewById<ComposeView>(R.id.aboutProjectTitle)
        aboutProjectTitle.setContent {
            val context = LocalContext.current
            themeProvider.WithTheme {
                TextTitleSmall(
                    text = context.getString(R.string.about_project_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }

    private fun setWebsiteLabel(view: View) {
        val websiteLabel = view.findViewById<ComposeView>(R.id.websiteLabel)
        websiteLabel.setContent {
            val context = LocalContext.current
            themeProvider.WithTheme {
                TextTitleMedium(
                    text = context.getString(R.string.about_website_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }

    private fun setWebsiteText(view: View) {
        val website = view.findViewById<ComposeView>(R.id.website)
        website.setContent {
            val context = LocalContext.current
            themeProvider.WithTheme {
                TextBodyMedium(
                    text = context.getString(R.string.app_webpage_url),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }

    private fun setForumLabel(view: View) {
        val forumLabel = view.findViewById<ComposeView>(R.id.user_forum_label)
        forumLabel.setContent {
            val context = LocalContext.current
            themeProvider.WithTheme {
                TextTitleMedium(
                    text = context.getString(R.string.user_forum_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }

    private fun setForumText(view: View) {
        val forum = view.findViewById<ComposeView>(R.id.user_forum)
        forum.setContent {
            val context = LocalContext.current
            themeProvider.WithTheme {
                TextBodyMedium(
                    text = context.getString(R.string.user_forum_url),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }

    private fun setLibrariesTitle(view: View) {
        val librariesTitle = view.findViewById<ComposeView>(R.id.librariesTitle)
        librariesTitle.setContent {
            val context = LocalContext.current
            themeProvider.WithTheme {
                TextTitleSmall(
                    text = context.getString(R.string.about_libraries),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                    color = MainTheme.colors.secondary,
                )
            }
        }
    }

    private fun displayChangeLog() {
        findNavController().navigate(R.id.action_aboutScreen_to_changelogScreen)
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

    companion object {
        private val USED_LIBRARIES = arrayOf(
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

private data class Library(val name: String, val url: String, val license: String)

private class LibrariesAdapter(private val dataset: Array<Library>) :
    RecyclerView.Adapter<LibrariesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: MaterialTextView = view.findViewById(R.id.name)
        val license: MaterialTextView = view.findViewById(R.id.license)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.about_library, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        val library = dataset[index]
        holder.name.text = library.name
        holder.license.text = library.license
        holder.itemView.setOnClickListener {
            holder.itemView.context.openUrl(library.url)
        }
    }

    override fun getItemCount() = dataset.size
}
