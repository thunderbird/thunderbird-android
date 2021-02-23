package com.fsck.k9.ui.changelog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.loader.observeLoading
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
        val listView = view.findViewById<RecyclerView>(R.id.changelog_list)

        viewModel.changelogState.observeLoading(
            owner = viewLifecycleOwner,
            loadingView = view.findViewById(R.id.changelog_loading),
            errorView = view.findViewById(R.id.changelog_error),
            dataView = listView
        ) { changeLog ->
            listView.adapter = ChangelogAdapter(changeLog)
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
