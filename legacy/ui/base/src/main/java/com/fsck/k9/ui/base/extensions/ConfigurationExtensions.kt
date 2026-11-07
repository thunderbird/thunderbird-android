package com.fsck.k9.ui.base.extensions

import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

var Configuration.currentLocale: Locale
    get() = locales[0]
    set(value) {
        setLocales(createLocaleList(value, locales))
    }

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
