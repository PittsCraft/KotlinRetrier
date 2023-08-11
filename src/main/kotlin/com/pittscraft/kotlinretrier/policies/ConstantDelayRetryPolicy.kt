package com.pittscraft.kotlinretrier.policies

import com.pittscraft.kotlinretrier.model.AttemptFailure
import com.pittscraft.kotlinretrier.model.RetrierEvent
import com.pittscraft.kotlinretrier.model.RetryDecision
import com.pittscraft.kotlinretrier.model.RetryPolicy
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ConstantDelayRetryPolicy(private val delay: Duration = DEFAULT_DELAY): RetryPolicy {

    companion object {
        val DEFAULT_DELAY = 1.seconds
    }

    override fun delayFor(attemptFailure: AttemptFailure<*>): Duration {
        return delay
    }

    override fun shouldRetry(attemptFailure: AttemptFailure<*>): RetryDecision {
        return RetryDecision.Retry(delay)
    }

    override fun freshCopy(): RetryPolicy {
        return this
    }
}