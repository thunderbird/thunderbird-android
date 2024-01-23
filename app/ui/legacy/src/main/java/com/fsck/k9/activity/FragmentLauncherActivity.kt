package com.fsck.k9.activity

import android.os.Bundle
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity

// Currently not used
class FragmentLauncherActivity : K9Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLayout(R.layout.activity_fragment_launcher)

        when (val fragment = intent.getStringExtra(EXTRA_FRAGMENT)) {
            else -> throw IllegalArgumentException("Unknown destination: $fragment")
        }
    }

    companion object {
        const val EXTRA_FRAGMENT = "fragment"
    }
}
