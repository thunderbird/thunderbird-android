package com.fsck.k9.logging

/**
 * Our fake `Timber` object.
 */
object Timber {
    var logger: Logger = NoOpLogger()

    @JvmStatic
    fun v(message: String?, vararg args: Any?) {
        logger.v(message, *args)
    }

    @JvmStatic
    fun v(t: Throwable?, message: String?, vararg args: Any?) {
        logger.v(t, message, *args)
    }

    @JvmStatic
    fun v(t: Throwable?) {
        logger.v(t)
    }

    @JvmStatic
    fun d(message: String?, vararg args: Any?) {
        logger.d(message, *args)
    }

    @JvmStatic
    fun d(t: Throwable?, message: String?, vararg args: Any?) {
        logger.d(t, message, *args)
    }

    @JvmStatic
    fun d(t: Throwable?) {
        logger.d(t)
    }

    @JvmStatic
    fun i(message: String?, vararg args: Any?) {
        logger.i(message, *args)
    }

    @JvmStatic
    fun i(t: Throwable?, message: String?, vararg args: Any?) {
        logger.i(t, message, *args)
    }

    @JvmStatic
    fun i(t: Throwable?) {
        logger.i(t)
    }

    @JvmStatic
    fun w(message: String?, vararg args: Any?) {
        logger.w(message, *args)
    }

    @JvmStatic
    fun w(t: Throwable?, message: String?, vararg args: Any?) {
        logger.w(t, message, *args)
    }

    @JvmStatic
    fun w(t: Throwable?) {
        logger.w(t)
    }

    @JvmStatic
    fun e(message: String?, vararg args: Any?) {
        logger.e(message, *args)
    }

    @JvmStatic
    fun e(t: Throwable?, message: String?, vararg args: Any?) {
        logger.e(t, message, *args)
    }

    @JvmStatic
    fun e(t: Throwable?) {
        logger.e(t)
    }
}
