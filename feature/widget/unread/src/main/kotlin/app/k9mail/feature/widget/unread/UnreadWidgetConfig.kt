package app.k9mail.feature.widget.unread

interface UnreadWidgetConfig {
    val providerClass: Class<out BaseUnreadWidgetProvider>
}
