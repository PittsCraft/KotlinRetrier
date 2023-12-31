package com.pittscraft.kotlinretrier.policybuilding

import com.pittscraft.kotlinretrier.model.AttemptFailure
import com.pittscraft.kotlinretrier.model.RetrierEvent
import com.pittscraft.kotlinretrier.model.RetryDecision
import com.pittscraft.kotlinretrier.model.RetryPolicy
import java.time.Instant
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.DurationUnit

class GiveUpOnPolicyWrapper(
    private val wrapped: RetryPolicy,
    private val giveUpCriterium: (AttemptFailure<*>) -> Boolean
) : RetryPolicy {

    override fun delayFor(attemptFailure: AttemptFailure<*>): Duration {
        return wrapped.delayFor(attemptFailure)
    }

    override fun shouldRetry(attemptFailure: AttemptFailure<*>): RetryDecision {
        if (giveUpCriterium(attemptFailure)) {
            return RetryDecision.GiveUp
        }
        return wrapped.shouldRetry(attemptFailure)
    }

    override fun freshCopy(): RetryPolicy {
        return GiveUpOnPolicyWrapper(wrapped.freshCopy(), giveUpCriterium)
    }
}

fun RetryPolicy.giveUpOn(criterium: (AttemptFailure<*>) -> Boolean): RetryPolicy {
    return GiveUpOnPolicyWrapper(this, criterium)
}

fun RetryPolicy.giveUpAfterMaxAttempts(maxAttempts: UInt): RetryPolicy {
    return giveUpOn { it.index >= maxAttempts - 1u }
}

fun RetryPolicy.giveUpAfterTimeout(timeout: Duration): RetryPolicy {
    return GiveUpOnPolicyWrapper(this) {
        val nextAttemptStart = Instant.now().plusMillis(delayFor(it).toLong(DurationUnit.MILLISECONDS))
        nextAttemptStart >= it.trialStart.plusMillis(timeout.toLong(DurationUnit.MILLISECONDS))
    }
}

fun RetryPolicy.giveUpOnErrorsMatching(finalErrorCriterium: (Throwable) -> Boolean): RetryPolicy {
    return giveUpOn { finalErrorCriterium(it.error) }
}