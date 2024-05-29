package com.fsck.k9.ui.changelog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import app.k9mail.core.android.common.compat.BundleCompat
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.loader.observeLoading
import com.google.android.material.checkbox.MaterialCheckBox
import de.cketti.changelog.ReleaseItem
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * Displays the changelog entries in a scrolling list
 */
class ChangelogFragment : Fragment() {
    private val viewModel: ChangelogViewModel by viewModel {
        val mode = arguments?.let {
            BundleCompat.getSerializable(it, ARG_MODE, ChangeLogMode::class.java)
        } ?: error("Missing argument '$ARG_MODE'")
        parametersOf(mode)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_changelog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val listView = view.findViewById<RecyclerView>(R.id.changelog_list)

        viewModel.changelogState.observeLoading(
            owner = viewLifecycleOwner,
            loadingView = view.findViewById(R.id.changelog_loading),
            errorView = view.findViewById(R.id.changelog_error),
            dataView = listView,
        ) { changeLog ->
            listView.adapter = ChangelogAdapter(changeLog)
        }

        setUpShowRecentChangesCheckbox(view)
    }

    private fun setUpShowRecentChangesCheckbox(view: View) {
        val showRecentChangesCheckBox = view.findViewById<MaterialCheckBox>(R.id.show_recent_changes_checkbox)
        var isInitialValue = true
        viewModel.showRecentChangesState.observe(viewLifecycleOwner) { showRecentChanges ->
            showRecentChangesCheckBox.isChecked = showRecentChanges
            if (isInitialValue) {
                // Don't animate when setting initial value
                showRecentChangesCheckBox.jumpDrawablesToCurrentState()
                isInitialValue = false
            }
        }
        showRecentChangesCheckBox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowRecentChanges(isChecked)
        }
    }

    companion object {
        const val ARG_MODE = "mode"
    }
}

class ChangelogAdapter(releaseItems: List<ReleaseItem>) : RecyclerView.Adapter<ViewHolder>() {
    private val items = releaseItems.flatMap { listOf(it) + it.changes }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.changelog_list_release_item -> ReleaseItemViewHolder(view)
            R.layout.changelog_list_change_item -> ChangeItemViewHolder(view)
            else -> error("Unsupported view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ReleaseItem -> {
                val viewHolder = holder as ReleaseItemViewHolder
                val context = viewHolder.versionName.context
                viewHolder.versionName.text = context.getString(R.string.changelog_version_title, item.versionName)
                viewHolder.versionDate.text = item.date
            }

            is String -> {
                val viewHolder = holder as ChangeItemViewHolder
                viewHolder.changeText.text = item
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ReleaseItem -> R.layout.changelog_list_release_item
            is String -> R.layout.changelog_list_change_item
            else -> error("Unsupported item type: ${items[position]}")
        }
    }

    override fun getItemCount(): Int = items.size
}

class ReleaseItemViewHolder(view: View) : ViewHolder(view) {
    val versionName: TextView = view.findViewById(R.id.version_name)
    val versionDate: TextView = view.findViewById(R.id.version_date)
}

class ChangeItemViewHolder(view: View) : ViewHolder(view) {
    val changeText: TextView = view.findViewById(R.id.change_text)
}
