package app.k9mail.ui.widget.list

import android.content.Intent
import android.widget.RemoteViewsService

class MessageListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MessageListRemoteViewFactory(applicationContext)
    }
}
