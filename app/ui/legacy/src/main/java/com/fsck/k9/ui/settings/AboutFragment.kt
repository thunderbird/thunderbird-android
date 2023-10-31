package com.fsck.k9.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import timber.log.Timber

class AboutFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val versionTextView = view.findViewById<TextView>(R.id.version)
        versionTextView.text = getVersionNumber()

        val versionLayout = view.findViewById<View>(R.id.versionLayout)
        versionLayout.setOnClickListener { displayChangeLog() }

        val authorsLayout = view.findViewById<View>(R.id.authorsLayout)
        authorsLayout.setOnClickListener {
            openUrl(getString(R.string.app_authors_url))
        }

        val licenseLayout = view.findViewById<View>(R.id.licenseLayout)
        licenseLayout.setOnClickListener {
            openUrl(getString(R.string.app_license_url))
        }

        val sourceCodeLayout = view.findViewById<View>(R.id.sourceCodeLayout)
        sourceCodeLayout.setOnClickListener {
            openUrl(getString(R.string.app_source_url))
        }

        val websiteLayout = view.findViewById<View>(R.id.websiteLayout)
        websiteLayout.setOnClickListener {
            openUrl(getString(R.string.app_webpage_url))
        }

        val userForumLayout = view.findViewById<View>(R.id.userForumLayout)
        userForumLayout.setOnClickListener {
            openUrl(getString(R.string.user_forum_url))
        }

        val fediverseLayout = view.findViewById<View>(R.id.fediverseLayout)
        fediverseLayout.setOnClickListener {
            openUrl(getString(R.string.fediverse_url))
        }

        val manager = LinearLayoutManager(view.context)
        val librariesRecyclerView = view.findViewById<RecyclerView>(R.id.libraries)
        librariesRecyclerView.apply {
            layoutManager = manager
            adapter = LibrariesAdapter(USED_LIBRARIES)
            isNestedScrollingEnabled = false
            isFocusable = false
        }
    }

    private fun displayChangeLog() {
        findNavController().navigate(R.id.action_aboutScreen_to_changelogScreen)
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
            Library(
                name = "Android Jetpack libraries",
                url = "https://developer.android.com/jetpack",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "AndroidX Preference eXtended",
                url = "https://github.com/takisoft/preferencex-android",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "AppAuth for Android",
                url = "https://github.com/openid/AppAuth-Android",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "CircleImageView",
                url = "https://github.com/hdodenhof/CircleImageView",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "ckChangeLog",
                url = "https://github.com/cketti/ckChangeLog",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "Commons IO",
                url = "https://commons.apache.org/io/",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "FastAdapter",
                url = "https://github.com/mikepenz/FastAdapter",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "Glide",
                url = "https://github.com/bumptech/glide",
                license = "BSD, part MIT and Apache 2.0",
            ),
            Library(
                name = "jsoup",
                url = "https://jsoup.org/",
                license = "MIT License",
            ),
            Library(
                name = "jutf7",
                url = "http://jutf7.sourceforge.net/",
                license = "MIT License",
            ),
            Library(
                name = "JZlib",
                url = "http://www.jcraft.com/jzlib/",
                license = "BSD-style License",
            ),
            Library(
                name = "Koin",
                url = "https://insert-koin.io/",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "Kotlin Standard Library",
                url = "https://kotlinlang.org/api/latest/jvm/stdlib/",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "KotlinX coroutines",
                url = "https://github.com/Kotlin/kotlinx.coroutines",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "Material Components for Android",
                url = "https://github.com/material-components/material-components-android",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "Material Drawer",
                url = "https://github.com/mikepenz/MaterialDrawer",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "Mime4j",
                url = "https://james.apache.org/mime4j/",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "MiniDNS",
                url = "https://github.com/MiniDNS/minidns",
                license = "Multiple, Apache License, Version 2.0",
            ),
            Library(
                name = "Moshi",
                url = "https://github.com/square/moshi",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "OkHttp",
                url = "https://github.com/square/okhttp",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "Okio",
                url = "https://github.com/square/okio",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "SafeContentResolver",
                url = "https://github.com/cketti/SafeContentResolver",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "SearchPreference",
                url = "https://github.com/ByteHamster/SearchPreference",
                license = "MIT License",
            ),
            Library(
                name = "Timber",
                url = "https://github.com/JakeWharton/timber",
                license = "Apache License, Version 2.0",
            ),
            Library(
                name = "TokenAutoComplete",
                url = "https://github.com/splitwise/TokenAutoComplete/",
                license = "Apache License, Version 2.0",
            ),
        )
    }
}

private fun Fragment.openUrl(url: String) = requireContext().openUrl(url)

private fun Context.openUrl(url: String) {
    try {
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(viewIntent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, R.string.error_activity_not_found, Toast.LENGTH_SHORT).show()
    }
}

private data class Library(val name: String, val url: String, val license: String)

private class LibrariesAdapter(private val dataset: Array<Library>) :
    RecyclerView.Adapter<LibrariesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val license: TextView = view.findViewById(R.id.license)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.about_library, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        val library = dataset[index]
        holder.name.text = library.name
        holder.license.text = library.license
        holder.itemView.setOnClickListener {
            holder.itemView.context.openUrl(library.url)
        }
    }

    override fun getItemCount() = dataset.size
}
