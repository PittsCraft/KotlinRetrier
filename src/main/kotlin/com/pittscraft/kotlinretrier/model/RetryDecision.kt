package com.pittscraft.kotlinretrier.model

import kotlin.time.Duration

sealed class RetryDecision {
    data object GiveUp: RetryDecision()
    data class Retry(val delay: Duration): RetryDecision()
}