package com.pittscraft.kotlinretrier.model

import kotlin.time.Duration

interface RetryPolicy {
    fun delayFor(attemptFailure: AttemptFailure<*>): Duration

    fun shouldRetry(attemptFailure: AttemptFailure<*>): RetryDecision

    fun freshCopy(): RetryPolicy
}