package com.fsck.k9.ui.changelog

import android.os.Bundle
import android.view.MenuItem
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity

class RecentChangesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.activity_recent_changes)
        setTitle(R.string.changelog_recent_changes_title)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val fragment = ChangelogFragment().apply {
                arguments = bundleOf(ChangelogFragment.ARG_MODE to ChangeLogMode.RECENT_CHANGES)
            }
            supportFragmentManager.commit {
                add(R.id.fragment_container, fragment)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
