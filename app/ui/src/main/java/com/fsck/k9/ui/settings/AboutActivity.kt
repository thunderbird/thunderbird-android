package com.fsck.k9.ui.settings

import java.util.Calendar

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.ui.R
import de.cketti.library.changelog.ChangeLog

import timber.log.Timber

private data class Library(val name: String, val URL: String, val license: String = "")

class AboutActivity : K9Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.about)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        findViewById<TextView>(R.id.version).text = getVersionNumber()

        val year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR))
        findViewById<TextView>(R.id.copyright).text = getString(R.string.app_copyright_fmt, year, year)

        findViewById<View>(R.id.authorsLayout).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_authors_url))))
        }

        findViewById<View>(R.id.licenseLayout).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_license_url))))
        }

        findViewById<View>(R.id.source).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_source_url))))
        }

        findViewById<View>(R.id.changelog).setOnClickListener {
            displayChangeLog()
        }

        findViewById<View>(R.id.revisions).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_revision_url))))
        }

        val manager = LinearLayoutManager(this)
        val libraries = findViewById<RecyclerView>(R.id.libraries)
        libraries.apply {
            layoutManager = manager
            adapter = LibrariesAdapter(USED_LIBRARIES)
            setNestedScrollingEnabled(false)
            setFocusable(false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.about_option, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.changelog -> displayChangeLog()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun displayChangeLog() {
        ChangeLog(this).fullLogDialog.show()
    }

    private fun getVersionNumber(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "Error getting PackageInfo")
            "?"
        }
    }

    companion object {
        private val USED_LIBRARIES = arrayOf(
                Library("Android Support Library", "https://developer.android.com/topic/libraries/support-library/index.html", "Apache License, Version 2.0"),
                Library("Android-Support-Preference-V7-Fix", "https://github.com/Gericop/Android-Support-Preference-V7-Fix", "Unlicense/Apache License, Version 2.0"),
                Library("ckChangeLog", "https://github.com/cketti/ckChangeLog", "Apache License, Version 2.0"),
                Library("Commons IO", "https://commons.apache.org/io/", "Apache License, Version 2.0"),
                Library("Glide", "https://github.com/bumptech/glide", "Mixed License"),
                Library("HoloColorPicker", "https://github.com/LarsWerkman/HoloColorPicker", "Apache License, Version 2.0"),
                Library("jsoup", "https://jsoup.org/", "MIT License"),
                Library("jutf7", "http://jutf7.sourceforge.net/", "MIT License"),
                Library("JZlib", "http://www.jcraft.com/jzlib/", "BSD-style License"),
                Library("Mime4j", "http://james.apache.org/mime4j/", "Apache License, Version 2.0"),
                Library("Moshi", "https://github.com/square/moshi", "Apache License, Version 2.0"),
                Library("Okio", "https://github.com/square/okio", "Apache License, Version 2.0"),
                Library("SafeContentResolver", "https://github.com/cketti/SafeContentResolver", "Apache License, Version 2.0"),
                Library("ShowcaseView", "https://github.com/amlcurran/ShowcaseView", "Apache License, Version 2.0"),
                Library("Timber", "https://github.com/JakeWharton/timber", "Apache License, Version 2.0"),
                Library("TokenAutoComplete", "https://github.com/splitwise/TokenAutoComplete/", "Apache License, Version 2.0"))

        fun start(context: Context) {
            val intent = Intent(context, AboutActivity::class.java)
            context.startActivity(intent)
        }
    }
}

private class LibrariesAdapter(private val dataset: Array<Library>)
        : RecyclerView.Adapter<LibrariesAdapter.ViewHolder>() {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.about_library, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        val library = dataset[index]
        holder.view.findViewById<TextView>(R.id.name).text = library.name
        holder.view.findViewById<TextView>(R.id.license).text = library.license
        holder.view.setOnClickListener {
            holder.view.getContext()
                .startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(library.URL)))
        }
    }

    override fun getItemCount() = dataset.size
}
