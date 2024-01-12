package com.pittscraft.kotlinretrier.model

import kotlinx.coroutines.flow.*

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

    fun onEach(action: suspend (RetrierEvent<Output>) -> Unit): SingleOutputRetrier<Output> = transform { value ->
        action(value)
        return@transform emit(value)
    }.singleOutput()
}