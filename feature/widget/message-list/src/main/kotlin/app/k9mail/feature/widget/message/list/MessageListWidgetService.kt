package app.k9mail.feature.widget.message.list

import android.content.Intent
import android.widget.RemoteViewsService

class MessageListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MessageListRemoteViewFactory(applicationContext)
    }
}
