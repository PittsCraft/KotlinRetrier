package com.pittscraft.kotlinretrier

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun wait(duration: Duration) {
    withContext(Dispatchers.IO) {
        delay(duration)
    }
}

class Expectation(private val description: String? = null, private val allowMultipleFulfillments: Boolean = false) {
    private val mutex = Mutex(locked = true)

    var isFulfilled = false
        private set
    fun fulfill() {
        if (isFulfilled && allowMultipleFulfillments) {
            return
        }
        if (!mutex.isLocked) {
            throw IllegalStateException("Cannot fulfill expectation \"$description\" twice")
        }
        isFulfilled = true
        mutex.unlock()
    }

    suspend fun awaitFulfillment(timeout: Duration = 5.seconds, joining: Job? = null) {
        try {
            withContext(Dispatchers.IO) {
                withTimeout(timeout) {
                    joining?.join()
                    mutex.lock()
                    mutex.unlock()
                }
            }
        } catch (e: Throwable) {
            fail("Failed to await fulfillment \"$description\"")
        }
    }
}