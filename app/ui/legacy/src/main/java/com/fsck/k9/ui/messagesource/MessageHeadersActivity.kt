package com.fsck.k9.ui.messagesource

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity

/**
 * Temporary Activity used until the fragment can be displayed in the main activity.
 */
class MessageHeadersActivity : K9Activity() {
    companion object {
        const val ARG_REFERENCE = "reference"

        fun createLaunchIntent(context: Context, messageReference: MessageReference): Intent {
            val intent = Intent(context, MessageHeadersActivity::class.java)
            intent.putExtra(ARG_REFERENCE, messageReference.toIdentityString())
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.message_view_headers_activity)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val messageReference = MessageReference.parse(intent.getStringExtra(ARG_REFERENCE))
        val fragment = MessageHeadersFragment.newInstance(messageReference!!)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.message_headers_fragment, fragment)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
