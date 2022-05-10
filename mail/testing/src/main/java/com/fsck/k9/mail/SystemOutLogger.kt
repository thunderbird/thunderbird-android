package com.fsck.k9.mail

import com.fsck.k9.logging.Logger

class SystemOutLogger : Logger {
    override fun v(message: String?, vararg args: Any?) {
        System.out.printf("V/ ${message.orEmpty()}\n", *args)
    }

    override fun v(t: Throwable?, message: String?, vararg args: Any?) {
        t?.printStackTrace(System.out)
        v(message, *args)
    }

    override fun v(t: Throwable?) {
        t?.printStackTrace(System.out)
    }

    override fun d(message: String?, vararg args: Any?) {
        System.out.printf("D/ ${message.orEmpty()}\n", *args)
    }

    override fun d(t: Throwable?, message: String?, vararg args: Any?) {
        t?.printStackTrace(System.out)
        d(message, *args)
    }

    override fun d(t: Throwable?) {
        t?.printStackTrace(System.out)
    }

    override fun i(message: String?, vararg args: Any?) {
        System.out.printf("I/ ${message.orEmpty()}\n", *args)
    }

    override fun i(t: Throwable?, message: String?, vararg args: Any?) {
        t?.printStackTrace(System.out)
        i(message, *args)
    }

    override fun i(t: Throwable?) {
        t?.printStackTrace(System.out)
    }

    override fun w(message: String?, vararg args: Any?) {
        System.out.printf("W/ ${message.orEmpty()}\n", *args)
    }

    override fun w(t: Throwable?, message: String?, vararg args: Any?) {
        t?.printStackTrace(System.out)
        w(message, *args)
    }

    override fun w(t: Throwable?) {
        t?.printStackTrace(System.out)
    }

    override fun e(message: String?, vararg args: Any?) {
        System.out.printf("E/ ${message.orEmpty()}\n", *args)
    }

    override fun e(t: Throwable?, message: String?, vararg args: Any?) {
        t?.printStackTrace(System.out)
        e(message, *args)
    }

    override fun e(t: Throwable?) {
        t?.printStackTrace(System.out)
    }
}
