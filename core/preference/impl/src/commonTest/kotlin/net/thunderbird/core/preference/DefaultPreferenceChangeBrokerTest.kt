package net.thunderbird.core.preference

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import kotlin.test.Test

class DefaultPreferenceChangeBrokerTest {

    @Test
    fun `subscribe should add subscriber`() {
        val subscriber = PreferenceChangeSubscriber { }
        val subscribers = mutableSetOf<PreferenceChangeSubscriber>()
        val broker = DefaultPreferenceChangeBroker(subscribers)

        broker.subscribe(subscriber)

        assertThat(subscribers.size).isEqualTo(1)
        assertThat(subscribers).contains(subscriber)
    }

    @Test
    fun `unsubscribe should remove subscriber`() {
        val subscriber = PreferenceChangeSubscriber { }
        val subscribers = mutableSetOf<PreferenceChangeSubscriber>(subscriber)
        val broker = DefaultPreferenceChangeBroker(subscribers)

        broker.unsubscribe(subscriber)

        assertThat(subscribers.size).isEqualTo(0)
        assertThat(subscribers).doesNotContain(subscriber)
    }

    @Test
    fun `publish should notify subscribers`() {
        var received = false
        val subscriber = PreferenceChangeSubscriber { received = true }
        var receivedOther = false
        val otherSubscriber = PreferenceChangeSubscriber { receivedOther = true }
        val subscribers = mutableSetOf<PreferenceChangeSubscriber>(subscriber, otherSubscriber)
        val broker = DefaultPreferenceChangeBroker(subscribers)

        broker.publish()

        assertThat(received).isEqualTo(true)
        assertThat(receivedOther).isEqualTo(true)
    }

    @Test
    fun `publish should notify subscribers with correct scope`() {
        var receivedScope: PreferenceScope? = null

        val subscriber = PreferenceChangeSubscriber { scope ->
            receivedScope = scope
        }

        val broker = DefaultPreferenceChangeBroker(mutableSetOf(subscriber))

        broker.publish(PreferenceScope.NOTIFICATION)

        assertThat(receivedScope).isEqualTo(PreferenceScope.NOTIFICATION)
    }

    @Test
    fun `subscribe should not duplicate subscriber`() {
        val subscriber = PreferenceChangeSubscriber { }
        val subscribers = mutableSetOf<PreferenceChangeSubscriber>()
        val broker = DefaultPreferenceChangeBroker(subscribers)

        broker.subscribe(subscriber)
        broker.subscribe(subscriber)

        assertThat(subscribers.size).isEqualTo(1)
    }

    @Test
    fun `unsubscribe should not fail if subscriber not present`() {
        val subscriber = PreferenceChangeSubscriber { }
        val broker = DefaultPreferenceChangeBroker(mutableSetOf())

        broker.unsubscribe(subscriber)

        assertThat(true).isEqualTo(true)
    }

    @Test
    fun `publish should handle subscriber removal during iteration`() {
        val subscribers = mutableSetOf<PreferenceChangeSubscriber>()

        lateinit var subscriber: PreferenceChangeSubscriber
        subscriber = PreferenceChangeSubscriber {
            subscribers.remove(subscriber)
        }

        subscribers.add(subscriber)

        val broker = DefaultPreferenceChangeBroker(subscribers)

        broker.publish()

        assertThat(true).isEqualTo(true)
    }
}
