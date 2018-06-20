package com.fsck.k9.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import de.cketti.library.changelog.ChangeLog
import timber.log.Timber
import java.util.Calendar

class AboutActivity : K9Activity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
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
        val appName = getString(R.string.app_name)
        val year = Calendar.getInstance().get(Calendar.YEAR)

        val html = StringBuilder()
                .append("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />")
                .append("<img src=\"file:///android_asset/icon.png\" alt=\"").append(appName).append("\"/>")
                .append("<h1>")
                .append(getString(R.string.about_title_fmt,
                        "<a href=\"${ getString(R.string.app_webpage_url) }\">"))
                .append(appName)
                .append("</a>")
                .append("</h1><p>")
                .append(appName)
                .append(" ")
                .append(getString(R.string.debug_version_fmt, getVersionNumber()))
                .append("</p><p>")
                .append(getString(R.string.app_authors_fmt, getString(R.string.app_authors)))
                .append("</p><p>")
                .append(getString(R.string.app_revision_fmt,
                        "<a href=\"${ getString(R.string.app_revision_url) }\">" +
                                getString(R.string.app_revision_url) +
                                "</a>"))
                .append("</p><hr/><p>")
                .append(getString(R.string.app_copyright_fmt, Integer.toString(year), Integer.toString(year)))
                .append("</p><hr/><p>")
                .append(getString(R.string.app_license))
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
