package com.fsck.k9.ui.settings

import java.util.Calendar

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.ui.R
import de.cketti.library.changelog.ChangeLog

import timber.log.Timber

class AboutActivity : K9Activity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.about)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        findViewById<TextView>(R.id.version).text = getVersionNumber()

        val year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR))
        findViewById<TextView>(R.id.copyright).text = getString(R.string.app_copyright_fmt, year, year)

        findViewById<View>(R.id.licenseLayout).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_license_url))))
        }

        findViewById<View>(R.id.source).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_source_url))))
        }

        webView = findViewById(R.id.about_view)

        val aboutHtml = buildHtml()
        webView.loadDataWithBaseURL("file:///android_res/drawable/", aboutHtml, "text/html", "utf-8", null)
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

    private fun buildHtml(): String {
        val html = StringBuilder()
                .append("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />")
                .append("<p>")
                .append(getString(R.string.app_revision_fmt,
                        "<a href=\"${ getString(R.string.app_revision_url) }\">" +
                                getString(R.string.app_revision_url) +
                                "</a>"))
                .append("</p><hr/><p>")

        val libs = StringBuilder().append("<ul>")
        for ((library, url) in USED_LIBRARIES) {
            libs.append("<li><a href=\"").append(url).append("\">")
                    .append(library)
                    .append("</a></li>")
        }
        libs.append("</ul>")

        html.append(getString(R.string.app_libraries, libs.toString()))
                .append("</p>")

        return html.toString()
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
        private val USED_LIBRARIES = mapOf(
                "Android Support Library" to "https://developer.android.com/topic/libraries/support-library/index.html",
                "Android-Support-Preference-V7-Fix" to "https://github.com/Gericop/Android-Support-Preference-V7-Fix",
                "ckChangeLog" to "https://github.com/cketti/ckChangeLog",
                "Commons IO" to "http://commons.apache.org/io/",
                "Glide" to "https://github.com/bumptech/glide",
                "HoloColorPicker" to "https://github.com/LarsWerkman/HoloColorPicker",
                "jsoup" to "https://jsoup.org/",
                "jutf7" to "http://jutf7.sourceforge.net/",
                "JZlib" to "http://www.jcraft.com/jzlib/",
                "Mime4j" to "http://james.apache.org/mime4j/",
                "Moshi" to "https://github.com/square/moshi",
                "Okio" to "https://github.com/square/okio",
                "SafeContentResolver" to "https://github.com/cketti/SafeContentResolver",
                "ShowcaseView" to "https://github.com/amlcurran/ShowcaseView",
                "Timber" to "https://github.com/JakeWharton/timber",
                "TokenAutoComplete" to "https://github.com/splitwise/TokenAutoComplete/")

        fun start(context: Context) {
            val intent = Intent(context, AboutActivity::class.java)
            context.startActivity(intent)
        }
    }
}
