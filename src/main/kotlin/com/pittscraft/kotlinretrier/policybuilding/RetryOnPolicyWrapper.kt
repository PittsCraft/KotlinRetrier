package com.pittscraft.kotlinretrier.policybuilding

import com.pittscraft.kotlinretrier.model.AttemptFailure
import com.pittscraft.kotlinretrier.model.RetrierEvent
import com.pittscraft.kotlinretrier.model.RetryDecision
import com.pittscraft.kotlinretrier.model.RetryPolicy
import kotlin.time.Duration

class RetryOnPolicyWrapper(
    private val wrapped: RetryPolicy,
    private val retryCriterium: (AttemptFailure<*>) -> Boolean
): RetryPolicy {

    override fun delayFor(attemptFailure: AttemptFailure<*>): Duration {
        return wrapped.delayFor(attemptFailure)
    }

    override fun shouldRetry(attemptFailure: AttemptFailure<*>): RetryDecision {
        if (retryCriterium(attemptFailure)) {
            return RetryDecision.Retry(delayFor(attemptFailure))
        }
        return wrapped.shouldRetry(attemptFailure)
    }

    override fun freshCopy(): RetryPolicy {
        return RetryOnPolicyWrapper(wrapped.freshCopy(), retryCriterium)
    }
}

fun RetryPolicy.retryOn(criterium: (AttemptFailure<*>) -> Boolean): RetryPolicy {
    return RetryOnPolicyWrapper(this, criterium)
}

fun RetryPolicy.retryOnErrorsMatching(criterium: (Throwable) -> Boolean): RetryPolicy {
    return retryOn { criterium(it.error) }
}