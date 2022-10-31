package app.k9mail.ui.widget.list

interface MessageListWidgetConfig {
    val providerClass: Class<out MessageListWidgetProvider>
}
