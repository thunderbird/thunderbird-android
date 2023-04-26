package app.k9mail.autodiscovery.providersxml

import android.content.Context
import android.content.res.XmlResourceParser

class ProvidersXmlProvider(private val context: Context) {
    fun getXml(): XmlResourceParser {
        return context.resources.getXml(R.xml.providers)
    }
}
