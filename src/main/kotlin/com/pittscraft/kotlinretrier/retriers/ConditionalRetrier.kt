package com.pittscraft.kotlinretrier.retriers

import com.pittscraft.kotlinretrier.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ConditionalRetrier<Output>(
    private val policy: RetryPolicy,
    private val conditionFlow: Flow<Boolean>,
    private val task: suspend () -> Output,
) : SingleOutputRetrier<Output> {

    private val flow: Flow<RetrierEvent<Output>>
        get() {
            var attempt = 0u
            var lastCondition: Boolean? = null
            val emptyWaitingFlow = MutableSharedFlow<RetrierEvent<Output>>()
            var lastRetrier: SimpleRetrier<Output>? = null
            return conditionFlow
                .distinctUntilChanged()
                // Catch clean completion of the condition flow and emit the special null value
                .map<Boolean, Boolean?> { it }
                .onCompletion {
                    if (it == null && lastCondition != true) {
                        emit(null)
                    }
                }
                .flatMapLatest {
                    val previousCondition = lastCondition
                    lastCondition = it
                    when (it) {
                        true -> {
                            val retrier = SimpleRetrier(policy, task)
                            lastRetrier = retrier
                            retrier.map { event ->
                                if (event is AttemptFailure) {
                                    val newEvent = AttemptFailure<Output>(event.trialStart, attempt, event.error)
                                    attempt++
                                    newEvent
                                } else {
                                    event
                                }
                            }
                        }
                        // If the false condition is interrupting a trial, emit an AttemptFailure then wait for the
                        // condition to change
                        false -> {
                            val retrier = lastRetrier
                            if (previousCondition == true && retrier != null) {
                                attempt++
                                flowOf(
                                    flowOf(
                                        AttemptFailure(retrier.trialStart, attempt - 1u, CancellationException())
                                    ),
                                    emptyWaitingFlow
                                ).flattenConcat()
                            } else {
                                // Else just wait for the condition to change
                                MutableSharedFlow()
                            }
                        }
                        // The condition flow stopped after either emitting false or nothing, the retrier will never
                        // finish naturally.
                        null -> {
                            flowOf(
                                Completion(ConditionFlowCompletedWithNoValueOrFalse())
                            )
                        }
                    }
                }
                .transformWhile {
                    emit(it)
                    it !is Completion
                }
        }

    override suspend fun collect(collector: FlowCollector<RetrierEvent<Output>>) {
        flow.collect(collector)
    }
}

class ConditionFlowCompletedWithNoValueOrFalse: CancellationException("Condition flow completed after either emitting false or nothing")