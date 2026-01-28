package com.fsck.k9.ui.messagelist

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.k9mail.core.ui.compose.common.mvi.observeWithoutEffect
import app.k9mail.core.ui.compose.designsystem.atom.DividerVertical
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.theme2.MainTheme
import com.fsck.k9.ui.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.ui.compose.designsystem.atom.ClickableSurface
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
import net.thunderbird.feature.mail.message.list.domain.model.SortType
import net.thunderbird.feature.mail.message.list.extension.toDomainSortType
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListMetadata
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.serialization.LocalMessageSearchSerializer
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parameterSetOf
import app.k9mail.core.ui.legacy.designsystem.R as DesignSystemR
import com.google.android.material.R as MaterialR

private const val TAG = "MessageListFragment"

// TODO(#10322): Move this fragment to :feature:mail:message:list once all migration to the new
//              MessageListFragment to MVI is done.
@SuppressLint("DiscouragedApi")
class MessageListFragment : BaseMessageListFragment() {
    override val logTag: String = TAG

    private val viewModel: MessageListContract.ViewModel by inject {
        decodeArguments()
        parameterSetOf(accountUuids.map { AccountIdFactory.of(it) }.toSet())
    }

    private val featureThemeProvider: FeatureThemeProvider by inject()

    internal val MessageListMetadata.currentSortCriteria: SortCriteria
        get() =
            sortCriteriaPerAccount.getValue(folder?.account?.id)

    override val swipeActions: StateFlow<Map<AccountId, SwipeActions>> by lazy {
        viewModel
            .state
            .map { it.metadata.swipeActions }
            .stateIn(lifecycleScope, SharingStarted.Lazily, emptyMap())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        // TODO(#10251): Required as the current implementation of sortType and sortAscending
                        //  returns null before we load the sort type. That should be removed when
                        //  the message list item's load is switched to the new state.
                        is MessageListEffect.RefreshMessageList -> {
                            val (primarySortType, secondarySortType) = effect.currentState.metadata.currentSortCriteria
                            val (sortType, sortAscending) = primarySortType.toDomainSortType()
                            updateCurrentSortCriteria(
                                sortType = sortType,
                                sortAscending = sortAscending,
                                sortDateAscending = when (primarySortType) {
                                    SortType.DateAsc -> true
                                    SortType.DateDesc -> false
                                    else -> secondarySortType == SortType.DateAsc
                                },
                            )
                            loadMessageList()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    override suspend fun fetchMessageListAppearance(): Flow<MessageListAppearance> = viewModel
        .state
        .mapNotNull { state -> state.preferences?.toMessageListAppearance() }

    override fun prepareSortMenu(menu: Menu) {
        menu.removeItem(R.id.set_sort)
        menu.findItem(R.id.select_sort_criteria).apply {
            isVisible = true
            actionView = MaterialButton(
                requireContext(),
                null,
                MaterialR.attr.materialIconButtonStyle,
            ).apply {
                val color = MaterialColors.getColor(this, MaterialR.attr.colorOnSurface)
                iconTint = ColorStateList.valueOf(color)
                icon = AppCompatResources.getDrawable(context, DesignSystemR.drawable.ic_sort)
                setOnClickListener {
                    showComposeDropdown(
                        anchor = it,
                        lifecycleOwner = viewLifecycleOwner,
                        stateOwner = this@MessageListFragment,
                    )
                }
            }
        }
    }

    override fun initializeSortSettings() {
        // The sort type settings is now loaded by the GetSortCriteriaPerAccount.
        // Therefore, we override this method with an empty implementation, removing the
        // legacy implementation.
    }

    private fun showComposeDropdown(anchor: View, lifecycleOwner: LifecycleOwner, stateOwner: SavedStateRegistryOwner) {
        val context = anchor.context
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(stateOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                featureThemeProvider.WithTheme {
                    SortCriteriaMenuList()
                }
            }
        }
        val parent = FrameLayout(context).apply {
            id = android.R.id.content
            compositionContext = createLifecycleAwareWindowRecomposer(lifecycle = lifecycle)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            addView(composeView)
        }
        val popup = PopupWindow(
            parent,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        )
        popup.showAsDropDown(anchor)
    }

    /**
     * Ideally, this should be landed on :feature:mail:message:list:impl, but until we don't complete
     * a full rewrite of the Message List on compose, it wouldn't bring much benefits.
     *
     * For now, leaving this here.
     */
    @Composable
    private fun SortCriteriaMenuList() {
        val (state, dispatch) = viewModel.observeWithoutEffect()
        val metadata = state.value.metadata
        val primarySortTypes = metadata.availablePrimarySortTypes
        val secondarySortTypes = metadata.availableSecondarySortTypes
        val currentSortCriteria = remember(metadata) {
            val folder = metadata.folder
            val accountId = folder?.account?.id
            metadata.sortCriteriaPerAccount.getValue(accountId)
        }

        Surface(
            modifier = Modifier.padding(MainTheme.spacings.default),
            color = MainTheme.colors.surfaceContainer,
            tonalElevation = MainTheme.elevations.level2,
            shape = MainTheme.shapes.medium,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
                modifier = Modifier.height(IntrinsicSize.Min),
            ) {
                SortTypeList(
                    sortTypes = primarySortTypes,
                    isSelected = { sortType -> currentSortCriteria.primary == sortType },
                    onSortTypeClick = { sortType ->
                        dispatch(
                            MessageListEvent.ChangeSortCriteria(
                                accountId = null,
                                sortCriteria = currentSortCriteria.copy(
                                    primary = sortType,
                                    secondary = when {
                                        sortType in SortCriteria.SecondaryNotRequiredForSortTypes -> null
                                        else -> SortType.DateDesc
                                    },
                                ),
                            ),
                        )
                    },
                )
                SecondarySortOptions(currentSortCriteria, secondarySortTypes, dispatch)
            }
        }
    }

    private fun MessageListPreferences.toMessageListAppearance(): MessageListAppearance = MessageListAppearance(
        previewLines = excerptLines,
        stars = showFavouriteButton,
        senderAboveSubject = senderAboveSubject,
        showContactPicture = showMessageAvatar,
        showingThreadedList = groupConversations,
        backGroundAsReadIndicator = colorizeBackgroundWhenRead,
        showAccountIndicator = isShowAccountIndicator,
        density = density,
        dateTimeFormat = dateTimeFormat,
    )

    companion object Factory : BaseMessageListFragment.Factory {
        override fun newInstance(
            search: LocalMessageSearch,
            isThreadDisplay: Boolean,
            threadedList: Boolean,
        ): MessageListFragment {
            val searchBytes = LocalMessageSearchSerializer.serialize(search)

            return MessageListFragment().apply {
                arguments = bundleOf(
                    ARG_SEARCH to searchBytes,
                    ARG_IS_THREAD_DISPLAY to isThreadDisplay,
                    ARG_THREADED_LIST to threadedList,
                )
            }
        }
    }
}

@Composable
private fun SecondarySortOptions(
    currentSortCriteria: SortCriteria,
    secondarySortTypes: ImmutableSet<SortType>,
    dispatch: (MessageListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = currentSortCriteria.secondary != null,
        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
        modifier = modifier,
    ) {
        if (currentSortCriteria.secondary != null) {
            Row(
                modifier = Modifier.wrapContentWidth(
                    align = Alignment.Start,
                    unbounded = true,
                ),
            ) {
                DividerVertical()
                SortTypeList(
                    sortTypes = secondarySortTypes,
                    isSelected = { sortType -> currentSortCriteria.secondary == sortType },
                    onSortTypeClick = { sortType ->
                        dispatch(
                            MessageListEvent.ChangeSortCriteria(
                                accountId = null,
                                sortCriteria = currentSortCriteria.copy(secondary = sortType),
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SortTypeList(
    sortTypes: ImmutableSet<SortType>,
    isSelected: (SortType) -> Boolean,
    onSortTypeClick: (SortType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(IntrinsicSize.Max)) {
        sortTypes.forEach { sortType ->
            SortTypeMenuItem(
                sortType = sortType,
                selected = isSelected(sortType),
                onClick = { onSortTypeClick(sortType) },
            )
        }
    }
}

@Composable
private fun SortTypeMenuItem(
    sortType: SortType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ClickableSurface(
        onClick = onClick,
        color = MainTheme.colors.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = MainTheme.spacings.default, horizontal = MainTheme.spacings.double),
        ) {
            TextBodyLarge(
                text = stringResource(sortType.labelResId),
                modifier = Modifier.weight(1f),
            )
            Crossfade(targetState = selected, modifier = Modifier.size(16.dp)) { selected ->
                if (selected) {
                    Icon(imageVector = Icons.Outlined.CheckCircle)
                }
            }
        }
    }
}
