package com.fsck.k9

class TestClock(initialTime: Long = 0L) : Clock {
    override var time: Long = initialTime
}
