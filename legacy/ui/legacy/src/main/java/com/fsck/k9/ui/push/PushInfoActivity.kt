package com.fsck.k9.ui.push

import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.commit
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity

class PushInfoActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.push_info_title)
        setLayout(R.layout.activity_push_info)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(Icons.Outlined.Close)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(R.id.fragment_container, PushInfoFragment())
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
