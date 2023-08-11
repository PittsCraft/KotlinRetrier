package com.pittscraft.kotlinretrier.policies

import com.pittscraft.kotlinretrier.model.AttemptFailure
import com.pittscraft.kotlinretrier.model.RetryDecision
import com.pittscraft.kotlinretrier.model.RetryPolicy
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.times

class ExponentialBackoffRetryPolicy(
    private val timeSlot: Duration = DEFAULT_TIMESLOT,
    private val maxDelay: Duration = DEFAULT_MAX_DELAY,
    private val jitter: ExponentialBackoffJitter = DEFAULT_JITTER
) : RetryPolicy {

    companion object {
        val DEFAULT_TIMESLOT = 500.milliseconds
        val DEFAULT_MAX_DELAY = 60.seconds
        val DEFAULT_JITTER = Full
    }

    private var previousDelay: Duration? = null

    fun noJitterDelay(attemptIndex: UInt): Duration {
        val maxSlots = 2.0.pow(attemptIndex.toDouble())
        return timeSlot * maxSlots
    }

    private fun randomDuration(lowerBound: Duration, upperBound: Duration): Duration {
        val millisecondsDelay = Random.nextLong(lowerBound.inWholeMilliseconds, upperBound.inWholeMilliseconds + 1)
        return millisecondsDelay.milliseconds
    }

    fun fullJitterDelay(attemptIndex: UInt): Duration {
        return randomDuration(0.milliseconds, noJitterDelay(attemptIndex))
    }

    fun decorrelatedJitterDelay(attemptIndex: UInt, growthFactor: Double): Duration {
        val delay = previousDelay?.let {
            val upperBound = (growthFactor * it).coerceAtLeast(timeSlot)
            randomDuration(timeSlot, upperBound)
        } ?: fullJitterDelay(attemptIndex)
        previousDelay = delay
        return delay
    }

    fun uncappedDelay(attemptIndex: UInt): Duration {
        return when (jitter) {
            None -> noJitterDelay(attemptIndex)
            Full -> fullJitterDelay(attemptIndex)
            is Decorrelated -> {
                decorrelatedJitterDelay(attemptIndex, jitter.growthFactor)
            }
        }
    }

    override fun delayFor(attemptFailure: AttemptFailure<*>): Duration {
        return uncappedDelay(attemptFailure.index).coerceAtMost(maxDelay)
    }

    override fun shouldRetry(attemptFailure: AttemptFailure<*>): RetryDecision {
        return RetryDecision.Retry(delayFor(attemptFailure))
    }

    override fun freshCopy(): RetryPolicy {
        return ExponentialBackoffRetryPolicy(timeSlot, maxDelay, jitter)
    }
}

sealed class ExponentialBackoffJitter

data object None : ExponentialBackoffJitter()
data object Full : ExponentialBackoffJitter()
data class Decorrelated(val growthFactor: Double = DEFAULT_GROWTH_FACTOR) : ExponentialBackoffJitter() {
    companion object {
        const val DEFAULT_GROWTH_FACTOR: Double = 3.0
    }
}