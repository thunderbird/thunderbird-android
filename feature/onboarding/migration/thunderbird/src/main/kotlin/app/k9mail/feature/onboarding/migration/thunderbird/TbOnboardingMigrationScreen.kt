package app.k9mail.feature.onboarding.migration.thunderbird

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.card.CardFilled
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import org.koin.compose.koinInject

@Composable
internal fun TbOnboardingMigrationScreen(
    onQrCodeScan: () -> Unit,
    onAddAccount: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    brandNameProvider: BrandNameProvider = koinInject(),
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        ResponsiveWidthContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(contentPadding),
            ) {
                AppTitleTopHeader(
                    title = brandNameProvider.brandName,
                )

                Spacer(
                    modifier = Modifier
                        .height(MainTheme.spacings.double)
                        .weight(1f),
                )

                AlreadyUsingThunderbirdCard(onQrCodeScan)

                Spacer(modifier = Modifier.height(MainTheme.spacings.triple))

                TextGroup(title = stringResource(R.string.onboarding_migration_thunderbird_new_account_title)) {
                    ButtonOutlined(
                        text = stringResource(R.string.onboarding_migration_thunderbird_new_account_button_text),
                        onClick = onAddAccount,
                        modifier = Modifier.testTagAsResourceId("onboarding_migration_new_account_button"),
                    )
                }

                TextGroup(title = stringResource(R.string.onboarding_migration_thunderbird_import_title)) {
                    ButtonOutlined(
                        text = stringResource(R.string.onboarding_migration_thunderbird_import_button_text),
                        onClick = onImport,
                        modifier = Modifier.testTagAsResourceId("ImportButton"),
                    )
                }

                Spacer(
                    modifier = Modifier
                        .height(MainTheme.spacings.double)
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AlreadyUsingThunderbirdCard(onQrCodeScan: () -> Unit) {
    TextCard(title = stringResource(R.string.onboarding_migration_thunderbird_qr_code_import_title)) {
        TextBodyMedium(
            text = stringResource(R.string.onboarding_migration_thunderbird_qr_code_import_text),
            modifier = Modifier
                .padding(bottom = MainTheme.spacings.double),
        )

        TextBodyMedium(
            text = stringResource(R.string.onboarding_migration_thunderbird_qr_code_import_instructions_intro),
        )

        BulletList(
            items = persistentListOf(
                stringResource(R.string.onboarding_migration_thunderbird_qr_code_import_instructions_bullet_1),
                stringResource(R.string.onboarding_migration_thunderbird_qr_code_import_instructions_bullet_2_v2),
            ),
            modifier = Modifier
                .padding(
                    top = MainTheme.spacings.half,
                    bottom = MainTheme.spacings.double,
                ),
        )

        ButtonFilled(
            text = stringResource(R.string.onboarding_migration_thunderbird_qr_code_import_button_text),
            onClick = onQrCodeScan,
            modifier = Modifier
                .testTagAsResourceId("QrCodeImportButton")
                .align(Alignment.CenterHorizontally),
        )

        ThunderbirdVersionNote(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = MainTheme.spacings.half),
        )
    }
}

@Composable
private fun ThunderbirdVersionNote(
    modifier: Modifier = Modifier,
) {
    val formatString = if (LocalInspectionMode.current) {
        // When called from Android Studio previews, stringResource() replaces format string placeholders. We work
        // around that by using a static string.
        "Import requires the latest version of Thunderbird Desktop 128. %s"
    } else {
        stringResource(R.string.onboarding_migration_thunderbird_qr_code_import_instructions_require_latest)
    }

    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(MainTheme.shapes.small)
            .clickable { context.launchLearnHowToUpdateThunderbird() }
            .semantics { role = Role.Button }
            .padding(MainTheme.spacings.double),
    ) {
        check("%s" in formatString) { "Placeholder needs to be exactly %s" }

        val prefix = formatString.substringBefore("%s")
        val suffix = formatString.substringAfter("%s")

        val text = buildAnnotatedString {
            val linkText = AnnotatedString(
                text = stringResource(
                    R.string.onboarding_migration_thunderbird_qr_code_import_instructions_learn_update,
                ),
                spanStyle = SpanStyle(
                    color = MainTheme.colors.primary,
                    textDecoration = TextDecoration.Underline,
                ),
            )

            append(prefix)
            append(linkText)
            append(suffix)
        }

        TextBodySmall(text)
    }
}

@Composable
private fun TextCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardFilled(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MainTheme.spacings.quadruple),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MainTheme.spacings.double),
        ) {
            TextTitleMedium(
                text = title,
                color = MainTheme.colors.primary,
                modifier = Modifier
                    .padding(bottom = MainTheme.spacings.double),
            )

            content()
        }
    }
}

@Composable
private fun TextGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(MainTheme.spacings.double),
    ) {
        TextTitleMedium(
            text = title,
            color = MainTheme.colors.primary,
            modifier = Modifier
                .padding(bottom = MainTheme.spacings.default),
        )

        content()
    }
}

@Composable
private fun BulletList(
    items: ImmutableList<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
    ) {
        for (item in items) {
            Row {
                TextBodyMedium(text = " \u2022 ")
                TextBodyMedium(text = item)
            }
        }
    }
}

private fun Context.launchLearnHowToUpdateThunderbird() {
    try {
        val url = getString(R.string.onboarding_migration_thunderbird_update_thunderbird_url)
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        startActivity(viewIntent)
    } catch (e: ActivityNotFoundException) {
        Log.d(e, "Failed to open URL")

        Toast.makeText(
            this,
            getString(R.string.onboarding_migration_thunderbird_link_open_error),
            Toast.LENGTH_SHORT,
        ).show()
    }
}
