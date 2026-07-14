package net.thunderbird.core.preference

class DefaultPreferenceChangeBroker(
    private val subscribers: MutableSet<PreferenceChangeSubscriber> = mutableSetOf(),
) : PreferenceChangeBroker, PreferenceChangePublisher {

    private val lock = Any()

    override fun subscribe(subscriber: PreferenceChangeSubscriber) {
        synchronized(lock) {
            subscribers.add(subscriber)
        }
    }

    override fun unsubscribe(subscriber: PreferenceChangeSubscriber) {
        synchronized(lock) {
            subscribers.remove(subscriber)
        }
    }

    override fun publish(scope: PreferenceScope) {
        val currentSubscribers = synchronized(lock) { HashSet(subscribers) }

        for (subscriber in currentSubscribers) {
            subscriber.receive(scope)
        }
    }
}
