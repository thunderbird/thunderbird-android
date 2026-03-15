package com.fsck.k9.ui.changelog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import app.k9mail.core.android.common.compat.BundleCompat
import kotlin.getValue
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * Displays the changelog entries in a scrolling list
 */
class ChangelogFragment : Fragment() {
    private val themeProvider: FeatureThemeProvider by inject()
    private val viewModel: ChangelogViewModel by viewModel {
        val mode = arguments?.let {
            BundleCompat.getSerializable(it, ARG_MODE, ChangeLogMode::class.java)
        } ?: error("Missing argument '$ARG_MODE'")
        parametersOf(mode)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
            )

            setContent {
                val (state, dispatch) = viewModel.observe {}
                themeProvider.WithTheme {
                    ChangelogScreen(
                        releaseItems = state.value.releaseItems,
                        showRecentChanges = state.value.showRecentChanges,
                        onShowRecentChangesCheck = {
                            dispatch(ChangelogContract.Event.OnShowRecentChangesCheck(state.value.showRecentChanges))
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val ARG_MODE = "mode"
    }
}
