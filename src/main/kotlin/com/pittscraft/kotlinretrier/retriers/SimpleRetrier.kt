package com.pittscraft.kotlinretrier.retriers

import com.pittscraft.kotlinretrier.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant

class SimpleRetrier<Output>(
    private var policy: RetryPolicy,
    private val task: suspend () -> Output
) : SingleOutputRetrier<Output> {

    val trialStart: Instant = Instant.now()

   private val flow = flow {
       val value = task()
       emit(AttemptSuccess(value))
       emit(Completion())
   }.retryWhen { cause, attempt ->
       val event = AttemptFailure<Output>(trialStart, attempt.toUInt(), cause)
       emit(event)
       when (val decision = policy.shouldRetry(event)) {
           RetryDecision.GiveUp -> {
               emit(Completion(cause))
               false
           }

           is RetryDecision.Retry -> {
               delay(decision.delay)
               true
           }
       }
   }

    override suspend fun collect(collector: FlowCollector<RetrierEvent<Output>>) {
        flow.collect(collector)
    }
}
