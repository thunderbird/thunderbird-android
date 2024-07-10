package com.fsck.k9.ui.base.extensions

import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import java.util.Locale

@Suppress("DEPRECATION")
var Configuration.currentLocale: Locale
    get() {
        return if (Build.VERSION.SDK_INT >= 24) {
            locales[0]
        } else {
            locale
        }
    }
    set(value) {
        if (Build.VERSION.SDK_INT >= 24) {
            setLocales(createLocaleList(value, locales))
        } else {
            setLocale(value)
        }
    }

@RequiresApi(24)
private fun createLocaleList(topLocale: Locale, otherLocales: LocaleList): LocaleList {
    if (!otherLocales.isEmpty && otherLocales[0] == topLocale) {
        return otherLocales
    }

    val locales = mutableListOf(topLocale)
    for (index in 0 until otherLocales.size()) {
        val currentLocale = otherLocales[index]
        if (currentLocale != topLocale) {
            locales.add(currentLocale)
        }
    }

    return LocaleList(*locales.toTypedArray())
}
