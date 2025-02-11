package app.k9mail.legacy.preferences

class DefaultSettingsChangeBroker(
    private val subscribers: MutableSet<SettingsChangeSubscriber> = mutableSetOf(),
) : SettingsChangeBroker, SettingsChangePublisher {

    private val lock = Any()

    override fun subscribe(subscriber: SettingsChangeSubscriber) {
        synchronized(lock) {
            subscribers.add(subscriber)
        }
    }

    override fun unsubscribe(subscriber: SettingsChangeSubscriber) {
        synchronized(lock) {
            subscribers.remove(subscriber)
        }
    }

    override fun publish() {
        val currentSubscribers = synchronized(lock) { HashSet(subscribers) }

        for (subscriber in currentSubscribers) {
            subscriber.receive()
        }
    }
}
