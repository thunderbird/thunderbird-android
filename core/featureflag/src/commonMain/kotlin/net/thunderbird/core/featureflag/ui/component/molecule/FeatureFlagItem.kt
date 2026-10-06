package net.thunderbird.core.featureflag.ui.component.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlin.random.Random
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.atom.Switch
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextBodySmall
import net.thunderbird.components.ui.bolt.atom.text.TextLabelSmall
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.core.featureflag.resources.Res
import net.thunderbird.core.featureflag.resources.feature_flag_default_value
import net.thunderbird.core.featureflag.resources.feature_flag_modified_indicator
import net.thunderbird.core.featureflag.resources.feature_flag_overridden
import org.jetbrains.compose.resources.stringResource

@Composable
fun FeatureFlagItem(
    key: FeatureFlagKey,
    overrides: ImmutableMap<FeatureFlagKey, Boolean>,
    pendingOverrides: ImmutableMap<FeatureFlagKey, Boolean>,
    flagEnabled: Boolean,
    isOverridden: Boolean,
    modifier: Modifier = Modifier,
    onToggleFlagChange: (FeatureFlagKey) -> Unit = {},
    showDivider: Boolean = true,
    readonly: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !readonly, role = Role.Switch, onClick = { onToggleFlagChange(key) })
                .padding(start = BoltTheme.spacings.default)
                .padding(vertical = BoltTheme.spacings.default),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.half),
            ) {
                val modifiedIndicator = stringResource(Res.string.feature_flag_modified_indicator)
                TextBodyLarge(
                    text = buildAnnotatedString {
                        if (pendingOverrides.containsKey(key)) {
                            withStyle(SpanStyle(color = BoltTheme.colors.error)) {
                                append(modifiedIndicator)
                            }
                        }
                        append(key.key)
                    },
                )
                key.description?.let { description -> TextBodySmall(text = description) }
                AnimatedVisibility(visible = isOverridden) {
                    TextLabelSmall(
                        text = buildAnnotatedString {
                            append(
                                stringResource(Res.string.feature_flag_overridden),
                            )
                            append(" ")
                            withStyle(SpanStyle(color = BoltTheme.colors.info)) {
                                append(
                                    stringResource(
                                        Res.string.feature_flag_default_value,
                                        flagEnabled,
                                    ),
                                )
                            }
                        },
                    )
                }
            }
            if (!readonly) {
                Spacer(modifier = Modifier.width(BoltTheme.spacings.double))
                Switch(
                    checked = pendingOverrides[key] ?: overrides[key] ?: flagEnabled,
                    onCheckedChange = { onToggleFlagChange(key) },
                    modifier = Modifier.padding(end = BoltTheme.spacings.default),
                )
            }
        }
        if (showDivider) {
            DividerHorizontal()
        }
    }
}

private data class FeatureFlagItemPreviewParam(val readonly: Boolean)
private class FeatureFlagItemPreviewParamProvider : PreviewParameterProvider<FeatureFlagItemPreviewParam> {
    override val values: Sequence<FeatureFlagItemPreviewParam> = sequenceOf(
        FeatureFlagItemPreviewParam(readonly = true),
        FeatureFlagItemPreviewParam(readonly = false),
    )
}

@PreviewLightDark
@Composable
private fun Preview(@PreviewParameter(FeatureFlagItemPreviewParamProvider::class) param: FeatureFlagItemPreviewParam) {
    PreviewWithThemes {
        LazyColumn {
            val flags = GeneratedFeatureFlagKey.entries
            itemsIndexed(flags) { index, flag ->
                FeatureFlagItem(
                    key = flag,
                    overrides = persistentMapOf(),
                    pendingOverrides = persistentMapOf(),
                    flagEnabled = Random.nextBoolean(),
                    isOverridden = Random.nextBoolean(),
                    showDivider = flags.lastIndex != index,
                    readonly = param.readonly,
                    onToggleFlagChange = {},
                )
            }
        }
    }
}
