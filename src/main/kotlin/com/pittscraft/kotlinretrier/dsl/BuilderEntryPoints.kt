package com.pittscraft.kotlinretrier.dsl

import com.pittscraft.kotlinretrier.policies.ConstantDelayRetryPolicy
import com.pittscraft.kotlinretrier.policies.ExponentialBackoffJitter
import com.pittscraft.kotlinretrier.policies.ExponentialBackoffRetryPolicy
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun withExponentialBackoff(
    timeSlot: Duration = ExponentialBackoffRetryPolicy.DEFAULT_TIMESLOT,
    maxDelay: Duration = ExponentialBackoffRetryPolicy.DEFAULT_MAX_DELAY,
    jitter: ExponentialBackoffJitter = ExponentialBackoffRetryPolicy.DEFAULT_JITTER
): ColdRetrier {
    val policy = ExponentialBackoffRetryPolicy(timeSlot, maxDelay, jitter)
    return ColdRetrier(policy, null)
}

fun withConstantDelay(
    delay: Duration = ConstantDelayRetryPolicy.DEFAULT_DELAY
): ColdRetrier {
    val policy = ConstantDelayRetryPolicy(delay)
    return ColdRetrier(policy, null)
}

fun withNoDelay(): ColdRetrier {
    return withConstantDelay(0.seconds)
}
