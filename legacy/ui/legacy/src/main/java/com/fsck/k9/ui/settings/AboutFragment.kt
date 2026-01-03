package com.fsck.k9.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import com.fsck.k9.ui.R
import com.fsck.k9.ui.settings.AboutContract.Effect
import com.fsck.k9.ui.settings.AboutContract.Event
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject
import app.k9mail.core.ui.legacy.designsystem.R as DesignSystemR
import app.k9mail.core.ui.legacy.theme2.common.R as Theme2CommonR

class AboutFragment : Fragment() {
    private val themeProvider: FeatureThemeProvider by inject()
    private val appNameProvider: AppNameProvider by inject()
    private val viewModel: AboutViewModel by inject()

    @Suppress("LongMethod")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
            )
            val appLogoResId = resolveAppLogoResId(requireContext())
            val aboutTitle = context.getString(R.string.about_title, appNameProvider.appName)
            val projectTitle = context.getString(R.string.about_project_title)
            val librariesTitle = context.getString(R.string.about_libraries)

            setContent {
                val (state, dispatch) = viewModel.observe { effect ->
                    when (effect) {
                        is Effect.OpenChangeLog ->
                            findNavController()
                                .navigate(R.id.action_aboutScreen_to_changelogScreen)

                        is Effect.OpenUrl ->
                            context.openUrl(effect.url)
                    }
                }
                themeProvider.WithTheme {
                    AboutScreen(
                        versionNumber = state.value.version,
                        appLogoResId = appLogoResId,
                        libraries = state.value.libraries,
                        aboutTitle = aboutTitle,
                        projectTitle = projectTitle,
                        librariesTitle = librariesTitle,
                        displayChangeLog = {
                            dispatch(Event.OnChangeLogClick)
                        },
                        displayAuthors = {
                            dispatch(
                                Event.OnSectionContentClick(
                                    getString(R.string.app_authors_url),
                                ),
                            )
                        },
                        displayLicense = {
                            dispatch(
                                Event.OnSectionContentClick(
                                    getString(R.string.app_license_url),
                                ),
                            )
                        },
                        displayWebSite = {
                            dispatch(
                                Event.OnSectionContentClick(
                                    getString(R.string.app_webpage_url),
                                ),
                            )
                        },
                        displayForum = {
                            dispatch(
                                Event.OnSectionContentClick(
                                    getString(R.string.user_forum_url),
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

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
            .clickable(
                onClick = { context.openUrl(library.url) },
            )
            .padding(
                horizontal = MainTheme.spacings.double,
                vertical = MainTheme.spacings.oneHalf,
            )
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        TextTitleMedium(
            text = library.name,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )
        TextBodyMedium(
            text = library.license,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )
    }
}

@Composable
fun AboutScreen(
    versionNumber: String,
    aboutTitle: String,
    projectTitle: String,
    librariesTitle: String,
    appLogoResId: Int,
    libraries: ImmutableList<Library>,
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
            AppLogo(logoResId = appLogoResId)
            SectionTitle(title = aboutTitle)
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

            SectionTitle(title = projectTitle)

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

            SectionTitle(title = librariesTitle)
            LibraryList(libraries = libraries)
        }
    }
}

@Composable
fun AppLogo(logoResId: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = MainTheme.spacings.double),
        horizontalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = logoResId),
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
                start = MainTheme.spacings.double,
                end = MainTheme.spacings.double,
                top = MainTheme.spacings.double,
                bottom = MainTheme.spacings.default,
            ),
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
            .padding(horizontal = MainTheme.spacings.double, vertical = MainTheme.spacings.default)
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = sectionImageId),
            modifier = Modifier
                .size(MainTheme.sizes.icon),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(MainTheme.spacings.triple))
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
            )

            TextBodyMedium(
                text = sectionText,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            )

            secondarySectionText?.let {
                TextBodyMedium(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                )
            }
        }
    }
}

fun resolveAppLogoResId(context: Context): Int {
    val typedValue = TypedValue()
    val resolved = context.theme.resolveAttribute(
        Theme2CommonR.attr.appLogo,
        typedValue,
        true,
    )

    return if (resolved && typedValue.resourceId != 0) {
        typedValue.resourceId
    } else {
        0
    }
}
