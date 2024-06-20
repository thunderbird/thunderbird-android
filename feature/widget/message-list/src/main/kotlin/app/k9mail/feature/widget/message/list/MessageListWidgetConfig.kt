package app.k9mail.feature.widget.message.list

interface MessageListWidgetConfig {
    val providerClass: Class<out BaseMessageListWidgetProvider>
}
