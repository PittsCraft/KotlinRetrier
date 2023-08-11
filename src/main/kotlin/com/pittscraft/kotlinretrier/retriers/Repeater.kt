package com.pittscraft.kotlinretrier.retriers

import com.pittscraft.kotlinretrier.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

fun <Output>Repeater(policy: RetryPolicy, delay: Duration, task: suspend () -> Output) =
    Repeater(delay = delay, innerRetrier = SimpleRetrier(policy, task))

fun <Output>Repeater(
    policy: RetryPolicy,
    conditionFlow: Flow<Boolean>,
    delay: Duration,
    task: suspend () -> Output
) = Repeater(delay = delay, innerRetrier = ConditionalRetrier(policy, conditionFlow, task))

class Repeater<Output>(
    private val delay: Duration,
    private val innerRetrier: SingleOutputRetrier<Output>
) : Retrier<Output> {

    private val flow: Flow<RetrierEvent<Output>> = flow {
        var finished = false
        while (!finished) {
            innerRetrier
                .collect {
                    if (it is Completion) {
                        if (it.error != null) {
                            emit(it)
                            finished = true
                        }
                    } else {
                        emit(it)
                    }
                }
            delay(delay)
        }
    }

    override suspend fun collect(collector: FlowCollector<RetrierEvent<Output>>) {
        flow.collect(collector)
    }
}