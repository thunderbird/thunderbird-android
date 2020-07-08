package com.fsck.k9.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import de.cketti.library.changelog.ChangeLog
import java.util.Calendar
import kotlinx.android.synthetic.main.about_library.view.*
import kotlinx.android.synthetic.main.fragment_about.*
import timber.log.Timber

private data class Library(val name: String, val URL: String, val license: String)

class AboutFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        version.text = getVersionNumber()
        versionLayout.setOnClickListener { displayChangeLog() }

        val year = Calendar.getInstance().get(Calendar.YEAR).toString()
        copyright.text = getString(R.string.app_copyright_fmt, year, year)

        authorsLayout.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_authors_url))))
        }

        licenseLayout.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_license_url))))
        }

        source.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_source_url))))
        }

        changelog.setOnClickListener { displayChangeLog() }

        revisions.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_revision_url))))
        }

        val manager = LinearLayoutManager(view.context)
        libraries.apply {
            layoutManager = manager
            adapter = LibrariesAdapter(USED_LIBRARIES)
            isNestedScrollingEnabled = false
            isFocusable = false
        }
    }

    private fun displayChangeLog() {
        ChangeLog(requireActivity()).fullLogDialog.show()
    }

    private fun getVersionNumber(): String {
        return try {
            val context = requireContext()
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "Error getting PackageInfo")
            "?"
        }
    }

    companion object {
        private val USED_LIBRARIES = arrayOf(
            Library("Android Jetpack libraries", "https://developer.android.com/jetpack", "Apache License, Version 2.0"),
            Library("AndroidX Preference eXtended", "https://github.com/Gericop/Android-Support-Preference-V7-Fix", "Apache License, Version 2.0"),
            Library("CircleImageView", "https://github.com/hdodenhof/CircleImageView", "Apache License, Version 2.0"),
            Library("ckChangeLog", "https://github.com/cketti/ckChangeLog", "Apache License, Version 2.0"),
            Library("Commons IO", "https://commons.apache.org/io/", "Apache License, Version 2.0"),
            Library("FastAdapter", "https://github.com/mikepenz/FastAdapter", "Apache License, Version 2.0"),
            Library("Glide", "https://github.com/bumptech/glide", "BSD, part MIT and Apache 2.0"),
            Library("Groupie", "https://github.com/lisawray/groupie", "MIT License"),
            Library("jsoup", "https://jsoup.org/", "MIT License"),
            Library("jutf7", "http://jutf7.sourceforge.net/", "MIT License"),
            Library("JZlib", "http://www.jcraft.com/jzlib/", "BSD-style License"),
            Library("Kotlin Standard Library", "https://kotlinlang.org/api/latest/jvm/stdlib/", "Apache License, Version 2.0"),
            Library("KotlinX coroutines", "https://github.com/Kotlin/kotlinx.coroutines", "Apache License, Version 2.0"),
            Library("Material Components for Android", "https://github.com/material-components/material-components-android", "Apache License, Version 2.0"),
            Library("Material Drawer", "https://github.com/mikepenz/MaterialDrawer", "Apache License, Version 2.0"),
            Library("Mime4j", "https://james.apache.org/mime4j/", "Apache License, Version 2.0"),
            Library("MiniDNS", "https://github.com/MiniDNS/minidns", "Multiple, Apache License, Version 2.0"),
            Library("Moshi", "https://github.com/square/moshi", "Apache License, Version 2.0"),
            Library("OkHttp", "https://github.com/square/okhttp", "Apache License, Version 2.0"),
            Library("Okio", "https://github.com/square/okio", "Apache License, Version 2.0"),
            Library("SafeContentResolver", "https://github.com/cketti/SafeContentResolver", "Apache License, Version 2.0"),
            Library("SearchPreference", "https://github.com/ByteHamster/SearchPreference", "MIT License"),
            Library("Timber", "https://github.com/JakeWharton/timber", "Apache License, Version 2.0"),
            Library("TokenAutoComplete", "https://github.com/splitwise/TokenAutoComplete/", "Apache License, Version 2.0")
        )
    }
}

private class LibrariesAdapter(private val dataset: Array<Library>) :
        RecyclerView.Adapter<LibrariesAdapter.ViewHolder>() {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.about_library, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        val library = dataset[index]
        holder.view.name.text = library.name
        holder.view.license.text = library.license
        holder.view.setOnClickListener {
            holder.view.context
                .startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(library.URL)))
        }
    }

    override fun getItemCount() = dataset.size
}
