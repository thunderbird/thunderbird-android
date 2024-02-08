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
                "Android Jetpack libraries",
                "https://developer.android.com/jetpack",
                "Apache License, Version 2.0",
            ),
            Library(
                "AndroidX Preference extended",
                "https://github.com/takisoft/preferencex-android",
                "Apache License, Version 2.0",
            ),
            Library("AppAuth for Android", "https://github.com/openid/AppAuth-Android", "Apache License, Version 2.0"),
            Library("Accompanist", "https://github.com/google/accompanist", "Apache License, Version 2.0"),
            Library("Apache HttpComponents", "https://hc.apache.org/", "Apache License, Version 2.0"),
            Library("CircleImageView", "https://github.com/hdodenhof/CircleImageView", "Apache License, Version 2.0"),
            Library("ckChangeLog", "https://github.com/cketti/ckChangeLog", "Apache License, Version 2.0"),
            Library("Commons IO", "https://commons.apache.org/io/", "Apache License, Version 2.0"),
            Library("ColorPicker", "https://github.com/gregkorossy/ColorPicker", "Apache License, Version 2.0"),
            Library("DateTimePicker", "https://github.com/gregkorossy/DateTimePicker", "Apache License, Version 2.0"),
            Library("Error Prone annotations", "https://github.com/google/error-prone", "Apache License, Version 2.0"),
            Library("FlexboxLayout", "https://github.com/google/flexbox-layout", "Apache License, Version 2.0"),
            Library("FastAdapter", "https://github.com/mikepenz/FastAdapter", "Apache License, Version 2.0"),
            Library("Glide", "https://github.com/bumptech/glide", "BSD, part MIT and Apache 2.0"),
            Library("jsoup", "https://jsoup.org/", "MIT License"),
            Library("jutf7", "http://jutf7.sourceforge.net/", "MIT License"),
            Library("JZlib", "http://www.jcraft.com/jzlib/", "BSD-style License"),
            Library("jcip-annotations", "https://jcip.net/", "Public License"),
            Library(
                "Jetbrains Annotations for JVM-based languages",
                "https://github.com/JetBrains/java-annotations",
                "Apache License, Version 2.0",
            ),
            Library(
                "Jetbrains Compose Runtime",
                "https://github.com/JetBrains/compose-multiplatform-core",
                "Apache License, Version 2.0",
            ),
            Library("Koin", "https://insert-koin.io/", "Apache License, Version 2.0"),
            Library("Kotlin Parcelize Runtime", "https://github.com/JetBrains/kotlin", "Apache License, Version 2.0"),
            Library(
                "KotlinX Coroutines",
                "https://github.com/Kotlin/kotlinx.coroutines",
                "Apache License, Version 2.0",
            ),
            Library(
                "Kotlin Standard Library",
                "https://kotlinlang.org/api/latest/jvm/stdlib/",
                "Apache License, Version 2.0",
            ),
            Library(
                "KotlinX Immutable Collections",
                "https://github.com/Kotlin/kotlinx.collections.immutable",
                "Apache License, Version 2.0",
            ),
            Library("KotlinX DateTime", "https://github.com/Kotlin/kotlinx-datetime", "Apache License, Version 2.0"),
            Library(
                "Kotlin Android Extensions Runtime",
                "https://github.com/JetBrains/kotlin/tree/master/plugins/android-extensions/android-extensions-runtime",
                "Apache License, Version 2.0",
            ),
            Library(
                "ListenableFuture",
                "https://github.com/google/guava",
                "Apache License, Version 2.0",
            ),
            Library(
                "Material Components for Android",
                "https://github.com/material-components/material-components-android",
                "Apache License, Version 2.0",
            ),
            Library("Material Drawer", "https://github.com/mikepenz/MaterialDrawer", "Apache License, Version 2.0"),
            Library("Mime4j", "https://james.apache.org/mime4j/", "Apache License, Version 2.0"),
            Library("MiniDNS", "https://github.com/MiniDNS/minidns", "Multiple, Apache License, Version 2.0"),
            Library("Moshi", "https://github.com/square/moshi", "Apache License, Version 2.0"),
            Library("OkHttp", "https://github.com/square/okhttp", "Apache License, Version 2.0"),
            Library("Okio", "https://github.com/square/okio", "Apache License, Version 2.0"),
            Library(
                "SafeContentResolver",
                "https://github.com/cketti/SafeContentResolver",
                "Apache License, Version 2.0",
            ),
            Library("SearchPreference", "https://github.com/ByteHamster/SearchPreference", "MIT License"),
            Library("SLF4J", "https://www.slf4j.org/", "MIT License"),
            Library("Timber", "https://github.com/JakeWharton/timber", "Apache License, Version 2.0"),
            Library(
                "TokenAutoComplete",
                "https://github.com/splitwise/TokenAutoComplete/",
                "Apache License, Version 2.0",
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
