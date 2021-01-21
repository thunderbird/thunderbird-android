package com.fsck.k9.ui.changelog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.fsck.k9.ui.R
import de.cketti.changelog.ReleaseItem
import org.koin.android.ext.android.inject

/**
 * Displays the changelog entries in a scrolling list
 */
class ChangelogFragment : Fragment() {
    private val viewModel: ChangelogViewModel by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_changelog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.changelogState.observe(viewLifecycleOwner, StateObserver(view))
    }

    class StateObserver(view: View) : Observer<ChangeLogState> {
        private val loadingView: View = view.findViewById(R.id.changelog_loading)
        private val errorView: View = view.findViewById(R.id.changelog_error)
        private val listView: RecyclerView = view.findViewById(R.id.changelog_list)
        private val allViews = setOf(loadingView, errorView, listView)

        override fun onChanged(state: ChangeLogState?) {
            when (state) {
                is ChangeLogState.Loading -> loadingView.show()
                is ChangeLogState.Error -> errorView.show()
                is ChangeLogState.Data -> showChangelog(state.changeLog)
            }
        }

        private fun showChangelog(changeLog: List<ReleaseItem>) {
            listView.adapter = ChangelogAdapter(changeLog)
            listView.show()
        }

        private fun View.show() {
            for (view in allViews) {
                view.isVisible = view === this
            }
        }
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
