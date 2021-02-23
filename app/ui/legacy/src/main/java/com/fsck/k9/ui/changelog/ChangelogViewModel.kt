package com.fsck.k9.ui.changelog

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.fsck.k9.ui.base.loader.LoaderState
import com.fsck.k9.ui.base.loader.liveDataLoader
import de.cketti.changelog.ChangeLog
import de.cketti.changelog.ReleaseItem

private typealias ChangeLogState = LoaderState<List<ReleaseItem>>

class ChangelogViewModel(private val context: Context) : ViewModel() {
    val changelogState: LiveData<ChangeLogState> = liveDataLoader {
        val changeLog = ChangeLog.newInstance(context)
        changeLog.changeLog
    }
}
