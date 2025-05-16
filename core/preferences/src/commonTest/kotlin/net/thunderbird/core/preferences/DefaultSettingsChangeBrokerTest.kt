package net.thunderbird.core.preferences

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import kotlin.test.Test

class DefaultSettingsChangeBrokerTest {

    @Test
    fun `subscribe should add subscriber`() {
        val subscriber = SettingsChangeSubscriber { }
        val subscribers = mutableSetOf<SettingsChangeSubscriber>()
        val broker = DefaultSettingsChangeBroker(subscribers)

        broker.subscribe(subscriber)

        assertThat(subscribers.size).isEqualTo(1)
        assertThat(subscribers).contains(subscriber)
    }

    @Test
    fun `unsubscribe should remove subscriber`() {
        val subscriber = SettingsChangeSubscriber { }
        val subscribers = mutableSetOf<SettingsChangeSubscriber>(subscriber)
        val broker = DefaultSettingsChangeBroker(subscribers)

        broker.unsubscribe(subscriber)

        assertThat(subscribers.size).isEqualTo(0)
        assertThat(subscribers).doesNotContain(subscriber)
    }

    @Test
    fun `publish should notify subscribers`() {
        var received = false
        val subscriber = SettingsChangeSubscriber { received = true }
        var receivedOther = false
        val otherSubscriber = SettingsChangeSubscriber { receivedOther = true }
        val subscribers = mutableSetOf<SettingsChangeSubscriber>(subscriber, otherSubscriber)
        val broker = DefaultSettingsChangeBroker(subscribers)

        broker.publish()

        assertThat(received).isEqualTo(true)
        assertThat(receivedOther).isEqualTo(true)
    }
}
