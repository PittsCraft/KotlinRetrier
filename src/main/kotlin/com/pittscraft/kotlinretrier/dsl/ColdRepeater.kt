package com.pittscraft.kotlinretrier.dsl

import com.pittscraft.kotlinretrier.model.AttemptFailure
import com.pittscraft.kotlinretrier.model.RetrierEvent
import com.pittscraft.kotlinretrier.model.RetryPolicy
import com.pittscraft.kotlinretrier.policybuilding.giveUpOn
import com.pittscraft.kotlinretrier.policybuilding.retryOn
import com.pittscraft.kotlinretrier.retriers.Repeater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Duration

class ColdRepeater(
    private val policy: RetryPolicy,
    private val delay: Duration,
    private val conditionFlow: Flow<Boolean>?
) {

    fun giveUpOn(criterium: (AttemptFailure<*>) -> Boolean): ColdRepeater {
        val policy = this.policy.giveUpOn(criterium)
        return ColdRepeater(policy, delay, conditionFlow)
    }

    fun giveUpAfterMaxAttempts(maxAttempts: UInt): ColdRepeater {
        return giveUpOn { it.index >= maxAttempts - 1u }
    }

    fun giveUpOnErrorsMatching(finalErrorCriterium: (Throwable) -> Boolean): ColdRepeater {
        return giveUpOn { finalErrorCriterium(it.error) }
    }

    fun retryOn(criterium: (AttemptFailure<*>) -> Boolean): ColdRepeater {
        val policy = this.policy.retryOn(criterium)
        return ColdRepeater(policy, delay, conditionFlow)
    }

    fun retryOnErrorsMatching(criterium: (Throwable) -> Boolean): ColdRepeater {
        return retryOn { criterium(it.error) }
    }
    fun onlyWhen(conditionFlow: Flow<Boolean>): ColdRepeater {
        var finalFlow = conditionFlow
        if (this.conditionFlow != null) {
            finalFlow = this.conditionFlow.combine(conditionFlow) { firstCondition, secondCondition ->
                firstCondition && secondCondition
            }
        }
        return ColdRepeater(policy, delay, finalFlow)
    }

    fun <Output>task(task: suspend () -> Output): Repeater<Output> {
        return conditionFlow?.let {
            Repeater(policy, conditionFlow, delay, task)
        } ?: Repeater(policy, delay, task)
    }
}