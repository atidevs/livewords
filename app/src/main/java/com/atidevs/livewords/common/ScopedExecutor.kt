package com.atidevs.livewords.common

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Custom executor holder which exposes a shutdown option
 * for canceling further execution
 */
class ScopedExecutor(private val executor: Executor) : Executor {

    private val isShutDown = AtomicBoolean()

    fun shutDown() {
        isShutDown.set(true)
    }

    override fun execute(command: Runnable?) {
        if (!isShutDown.get()) executor.execute(command)
    }
}