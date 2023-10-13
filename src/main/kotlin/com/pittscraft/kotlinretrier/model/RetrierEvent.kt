package com.pittscraft.kotlinretrier.model

import java.time.Instant

sealed class RetrierEvent<Output> {

    val success: AttemptSuccess<Output>?
        get() {
            if (this is AttemptSuccess) {
                return this
            }
            return null
        }

    val failure: AttemptFailure<Output>?
        get() {
            if (this is AttemptFailure) {
                return this
            }
            return null
        }

    val completion: Completion<Output>?
        get() {
            if (this is Completion) {
                return this
            }
            return null
        }

    internal val isFinalForSingleOutput: Boolean
        get() {
            if (this is AttemptSuccess) {
                return true
            }
            if (this is Completion && this.error != null) {
                return true
            }
            return false
        }

    internal val isFinalForRepeater: Boolean
        get() {
            return this is Completion && this.error != null
        }
}

data class AttemptFailure<Output>(val trialStart: Instant, val index: UInt, val error: Throwable) : RetrierEvent<Output>()
data class AttemptSuccess<Output>(val value: Output) : RetrierEvent<Output>()
data class Completion<Output>(val error: Throwable? = null) : RetrierEvent<Output>()