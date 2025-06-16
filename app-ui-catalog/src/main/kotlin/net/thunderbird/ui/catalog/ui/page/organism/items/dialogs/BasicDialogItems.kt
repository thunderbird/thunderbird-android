package net.thunderbird.ui.catalog.ui.page.organism.items.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineSmall
import app.k9mail.core.ui.compose.designsystem.organism.BasicDialog
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItem
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItemPadding
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionSubtitleItem

internal fun LazyGridScope.basicDialogs() {
    sectionHeaderItem("Basic dialogs")
    basicDialogContentAndButtonItem()
    basicDialogContentButtonHeadlineItem()
    basicDialogContentButtonHeadlineSupportingTextItem()
    basicDialogWithDividers()
    basicDialogComplexImplementation()
}

private fun LazyGridScope.basicDialogContentAndButtonItem() {
    basicDialogItem(
        sectionSubtitle = "Basic Dialog with content and buttons",
    ) {
        BasicDialog(
            onDismissRequest = { dismiss() },
            content = {
                TextBodyLarge("Dialog content")
            },
            buttons = { ButtonText(text = "Dismiss", onClick = { dismiss() }) },
            contentPadding = PaddingValues(horizontal = MainTheme.spacings.triple),
        )
    }
}

private fun LazyGridScope.basicDialogContentButtonHeadlineItem() {
    basicDialogItem(
        sectionSubtitle = "Basic Dialog with content, buttons and headline as text",
    ) {
        BasicDialog(
            headlineText = "Headline text",
            onDismissRequest = { dismiss() },
            content = {
                TextBodyLarge("Dialog content")
            },
            buttons = { ButtonText(text = "Dismiss", onClick = { dismiss() }) },
            contentPadding = PaddingValues(horizontal = MainTheme.spacings.triple),
        )
    }
}

private fun LazyGridScope.basicDialogContentButtonHeadlineSupportingTextItem() {
    basicDialogItem(
        sectionSubtitle = "Basic Dialog with content, buttons, headline and supporting text",
    ) {
        BasicDialog(
            headlineText = "Headline text",
            supportingText = "This is a supporting text",
            onDismissRequest = { dismiss() },
            content = {
                TextBodyLarge("Dialog content")
            },
            buttons = { ButtonText(text = "Dismiss", onClick = { dismiss() }) },
            contentPadding = PaddingValues(horizontal = MainTheme.spacings.triple),
        )
    }
}

private fun LazyGridScope.basicDialogWithDividers() {
    basicDialogItem(
        sectionSubtitle = "Basic Dialog with dividers",
    ) {
        BasicDialog(
            onDismissRequest = { dismiss() },
            headlineText = "Headline text",
            supportingText = "This is a supporting text",
            content = {
                TextBodyLarge("Dialog content")
            },
            buttons = { ButtonText(text = "Dismiss", onClick = { dismiss() }) },
            contentPadding = PaddingValues(all = MainTheme.spacings.triple),
            showDividers = true,
            dividerColor = MainTheme.colors.primary,
        )
    }
}

private fun LazyGridScope.basicDialogComplexImplementation() {
    basicDialogItem(
        sectionSubtitle = "Complex Basic dialog building",
    ) {
        BasicDialog(
            onDismissRequest = { dismiss() },
            content = { ComplexBasicDialogContent() },
            buttons = {
                ButtonText(text = "Cancel", onClick = { dismiss() })
                ButtonText(text = "Accept", onClick = { dismiss() })
            },
            headline = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    TextHeadlineSmall(text = "Reset settings?")
                }
            },
            supportingText = {
                TextBodyMedium(
                    text = "This will reset your app preferences back to their default settings. " +
                        "The following accounts will also be signed out:",
                    color = MainTheme.colors.onSurfaceVariant,
                )
            },
            showDividers = true,
        )
    }
}

@Composable
private fun ComplexBasicDialogContent(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = MainTheme.spacings.triple,
                end = MainTheme.spacings.triple,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        ) {
            Box(
                modifier = Modifier
                    .size(MainTheme.sizes.iconAvatar)
                    .background(color = MainTheme.colors.primary, shape = CircleShape),
            )
            Text(text = "Account 1")
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        ) {
            Box(
                modifier = Modifier
                    .size(MainTheme.sizes.iconAvatar)
                    .background(color = MainTheme.colors.primary, shape = CircleShape),
            )
            Text(text = "Account 2")
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        ) {
            Box(
                modifier = Modifier
                    .size(MainTheme.sizes.iconAvatar)
                    .background(color = MainTheme.colors.primary, shape = CircleShape),
            )
            Text(text = "Account 3")
        }
    }
}

private fun LazyGridScope.basicDialogItem(
    sectionSubtitle: String,
    dialog: @Composable BasicDialogItemScope.() -> Unit,
) {
    sectionSubtitleItem(sectionSubtitle)
    defaultItem {
        val scope = remember { BasicDialogItemScope() }
        ButtonFilled(
            text = "Show dialog",
            onClick = { scope.show() },
            modifier = Modifier.padding(defaultItemPadding()),
        )

        if (scope.showDialog.value) {
            scope.dialog()
        }
    }
}

private interface BasicDialogItemScope {
    val showDialog: State<Boolean>
    fun show()
    fun dismiss()

    companion object {
        operator fun invoke(): BasicDialogItemScope = object : BasicDialogItemScope {
            private val _showDialog = mutableStateOf(false)
            override val showDialog: State<Boolean> = _showDialog

            override fun show() {
                _showDialog.value = true
            }

            override fun dismiss() {
                _showDialog.value = false
            }
        }
    }
}
