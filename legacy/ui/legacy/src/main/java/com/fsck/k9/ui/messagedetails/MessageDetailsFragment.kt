package com.fsck.k9.ui.messagedetails

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuCompat
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.ui.folder.FolderIconProvider
import app.k9mail.ui.utils.bottomsheet.ToolbarBottomSheetDialog
import app.k9mail.ui.utils.bottomsheet.ToolbarBottomSheetDialogFragment
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.contacts.ContactPictureLoader
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.CryptoResultAnnotation
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.extensions.withArguments
import com.fsck.k9.ui.observe
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import net.thunderbird.core.logging.legacy.Log
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openintents.openpgp.util.OpenPgpIntentStarter

class MessageDetailsFragment : ToolbarBottomSheetDialogFragment() {
    private val viewModel: MessageDetailsViewModel by viewModel()
    private val addToContactsLauncher: AddToContactsLauncher by inject()
    private val showContactLauncher: ShowContactLauncher by inject()
    private val contactPictureLoader: ContactPictureLoader by inject()
    private val folderIconProvider: FolderIconProvider by inject { parametersOf(requireContext().theme) }

    private lateinit var messageReference: MessageReference
    private val itemAdapter = ItemAdapter<GenericItem>()

    // FIXME: Replace this with a mechanism that survives process death
    var cryptoResult: CryptoResultAnnotation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        messageReference = MessageReference.parse(arguments?.getString(ARG_REFERENCE))
            ?: error("Missing argument $ARG_REFERENCE")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.message_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cryptoResult?.let {
            viewModel.cryptoResult = it
        }

        val dialog = checkNotNull(dialog)
        dialog.isDismissWithAnimation = true

        val toolbar = checkNotNull(toolbar)
        toolbar.apply {
            title = getString(R.string.message_details_toolbar_title)
            navigationIcon = ContextCompat.getDrawable(requireContext(), Icons.Outlined.Close)

            setNavigationOnClickListener {
                dismissAllowingStateLoss()
            }
        }

        val progressBar = view.findViewById<ProgressBar>(R.id.message_details_progress)
        val errorView = view.findViewById<View>(R.id.message_details_error)
        val recyclerView = view.findViewById<RecyclerView>(R.id.message_details_list)

        initializeRecyclerView(recyclerView, dialog)

        viewModel.uiEvents.observe(this) { event ->
            when (event) {
                is MessageDetailEvent.ShowCryptoKeys -> showCryptoKeys(event.pendingIntent)
                MessageDetailEvent.SearchCryptoKeys -> searchCryptoKeys()
                MessageDetailEvent.ShowCryptoWarning -> showCryptoWarning()
            }
        }

        viewModel.uiState.observe(this) { state ->
            when (state) {
                MessageDetailsState.Loading -> {
                    progressBar.isVisible = true
                    errorView.isVisible = false
                    recyclerView.isVisible = false
                }

                MessageDetailsState.Error -> {
                    progressBar.isVisible = false
                    errorView.isVisible = true
                    recyclerView.isVisible = false
                }

                is MessageDetailsState.DataLoaded -> {
                    progressBar.isVisible = false
                    errorView.isVisible = false
                    recyclerView.isVisible = true
                    setMessageDetails(state.details, state.appearance)
                }
            }
        }

        viewModel.initialize(messageReference)
    }

    override fun onResume() {
        super.onResume()

        viewModel.reload()
    }

    private fun initializeRecyclerView(recyclerView: RecyclerView, dialog: ToolbarBottomSheetDialog) {
        // Don't allow dragging down the bottom sheet (by dragging the toolbar) unless the list is scrolled all the way
        // to the top.
        recyclerView.addOnScrollListener(
            object : OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    dialog.behavior.isDraggable = !recyclerView.canScrollVertically(-1)
                }
            },
        )

        recyclerView.adapter = FastAdapter.with(itemAdapter).apply {
            addEventHook(cryptoStatusClickEventHook)
            addEventHook(participantClickEventHook)
            addEventHook(addToContactsClickEventHook)
            addEventHook(overflowClickEventHook)
        }
    }

    private fun setMessageDetails(details: MessageDetailsUi, appearance: MessageDetailsAppearance) {
        val items = buildList {
            add(MessageDateItem(details.date ?: getString(R.string.message_details_missing_date)))

            addCryptoStatus(details)

            addParticipants(details.from, R.string.message_details_from_section_title, appearance)
            addParticipants(details.sender, R.string.message_details_sender_section_title, appearance)
            addParticipants(details.replyTo, R.string.message_details_replyto_section_title, appearance)

            add(MessageDetailsDividerItem())

            addParticipants(details.to, R.string.message_details_to_section_title, appearance)
            addParticipants(details.cc, R.string.message_details_cc_section_title, appearance)
            addParticipants(details.bcc, R.string.message_details_bcc_section_title, appearance)

            addFolderName(details.folder)
        }

        // Use list index as stable identifier. This means changes to the list may only update existing items or add
        // new items to the end of the list. Use place holder items (EmptyItem) if necessary.
        items.forEachIndexed { index, item ->
            item.identifier = index.toLong()
        }

        itemAdapter.setNewList(items)
    }

    private fun MutableList<GenericItem>.addCryptoStatus(details: MessageDetailsUi) {
        if (details.cryptoDetails != null) {
            add(CryptoStatusItem(details.cryptoDetails))
        } else {
            add(EmptyItem())
        }
    }

    private fun MutableList<GenericItem>.addParticipants(
        participants: List<Participant>,
        @StringRes title: Int,
        appearance: MessageDetailsAppearance,
    ) {
        if (participants.isNotEmpty()) {
            val extraText = if (participants.size > 1) participants.size.toString() else null
            add(SectionHeaderItem(title = getString(title), extra = extraText))

            for (participant in participants) {
                add(
                    ParticipantItem(
                        contactPictureLoader,
                        appearance.showContactPicture,
                        appearance.alwaysHideAddToContactsButton,
                        participant,
                    ),
                )
            }
        }
    }

    private fun MutableList<GenericItem>.addFolderName(folder: FolderInfoUi?) {
        if (folder != null) {
            val folderNameItem = FolderNameItem(
                displayName = folder.displayName,
                iconResourceId = folderIconProvider.getFolderIcon(folder.type),
            )
            add(folderNameItem)
        } else {
            add(EmptyItem())
        }
    }

    private val cryptoStatusClickEventHook = object : ClickEventHook<CryptoStatusItem>() {
        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return if (viewHolder is CryptoStatusItem.ViewHolder) {
                viewHolder.itemView
            } else {
                null
            }
        }

        override fun onClick(
            v: View,
            position: Int,
            fastAdapter: FastAdapter<CryptoStatusItem>,
            item: CryptoStatusItem,
        ) {
            if (item.cryptoDetails.isClickable) {
                viewModel.onCryptoStatusClicked()
            }
        }
    }

    private val participantClickEventHook = object : ClickEventHook<ParticipantItem>() {
        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return if (viewHolder is ParticipantItem.ViewHolder) {
                viewHolder.itemView
            } else {
                null
            }
        }

        override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<ParticipantItem>, item: ParticipantItem) {
            val contactLookupUri = item.participant.contactLookupUri ?: return
            showContact(contactLookupUri)
        }
    }

    private fun showContact(contactLookupUri: Uri) {
        showContactLauncher.launch(requireContext(), contactLookupUri)
    }

    private val addToContactsClickEventHook = object : ClickEventHook<ParticipantItem>() {
        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return if (viewHolder is ParticipantItem.ViewHolder) {
                viewHolder.menuAddContact
            } else {
                null
            }
        }

        override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<ParticipantItem>, item: ParticipantItem) {
            val address = item.participant.address
            addToContacts(address)
        }
    }

    private fun addToContacts(address: Address) {
        addToContactsLauncher.launch(context = requireContext(), name = address.personal, email = address.address)
    }

    private val overflowClickEventHook = object : ClickEventHook<ParticipantItem>() {
        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return if (viewHolder is ParticipantItem.ViewHolder) {
                viewHolder.menuOverflow
            } else {
                null
            }
        }

        override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<ParticipantItem>, item: ParticipantItem) {
            showOverflowMenu(v, item.participant)
        }
    }

    private fun showOverflowMenu(view: View, participant: Participant) {
        val popupMenu = PopupMenu(requireContext(), view).apply {
            inflate(R.menu.participant_overflow_menu)
        }

        val menu = popupMenu.menu
        MenuCompat.setGroupDividerEnabled(menu, true)

        if (participant.address.personal == null) {
            menu.findItem(R.id.copy_name_and_email_address).isVisible = false
        }

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            onOverflowMenuItemClick(item.itemId, participant)
            true
        }

        popupMenu.show()
    }

    private fun onOverflowMenuItemClick(itemId: Int, participant: Participant) {
        when (itemId) {
            R.id.compose_to -> composeMessageToAddress(participant.address)
            R.id.copy_email_address -> viewModel.onCopyEmailAddressToClipboard(participant)
            R.id.copy_name_and_email_address -> viewModel.onCopyNameAndEmailAddressToClipboard(participant)
        }
    }

    private fun composeMessageToAddress(address: Address) {
        // TODO: Use the identity this message was sent to as sender identity

        val intent = Intent(context, MessageCompose::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_EMAIL, arrayOf(address.toString()))
            putExtra(MessageCompose.EXTRA_ACCOUNT, messageReference.accountUuid)
        }

        dismissAllowingStateLoss()
        requireContext().startActivity(intent)
    }

    private fun showCryptoKeys(pendingIntent: PendingIntent) {
        try {
            OpenPgpIntentStarter.startIntentSender(requireActivity(), pendingIntent.intentSender)
        } catch (e: SendIntentException) {
            Log.e(e, "Error starting PendingIntent")
        }
    }

    private fun searchCryptoKeys() {
        setFragmentResult(FRAGMENT_RESULT_KEY, bundleOf(RESULT_ACTION to ACTION_SEARCH_KEYS))
        dismissAllowingStateLoss()
    }

    private fun showCryptoWarning() {
        setFragmentResult(FRAGMENT_RESULT_KEY, bundleOf(RESULT_ACTION to ACTION_SHOW_WARNING))
        dismissAllowingStateLoss()
    }

    companion object {
        private const val ARG_REFERENCE = "reference"

        const val FRAGMENT_RESULT_KEY = "messageDetailsResult"
        const val RESULT_ACTION = "action"
        const val ACTION_SEARCH_KEYS = "search_keys"
        const val ACTION_SHOW_WARNING = "show_warning"

        fun create(messageReference: MessageReference): MessageDetailsFragment {
            return MessageDetailsFragment().withArguments(
                ARG_REFERENCE to messageReference.toIdentityString(),
            )
        }
    }
}
