package com.pittscraft.kotlinretrier.model

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface SingleOutputRetrier<Output>: Retrier<Output> {

    suspend fun value(): Output {
        return map {
            when (it) {
                is AttemptFailure -> null
                is AttemptSuccess -> it
                is Completion -> {
                    if (it.error != null) {
                        throw it.error
                    }
                    null
                }
            }
        }
            .filterNotNull()
            .first()
            .value
    }
}