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
}
