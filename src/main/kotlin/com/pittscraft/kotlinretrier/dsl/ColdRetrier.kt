package com.pittscraft.kotlinretrier.dsl

import com.pittscraft.kotlinretrier.model.AttemptFailure
import com.pittscraft.kotlinretrier.model.RetryPolicy
import com.pittscraft.kotlinretrier.model.SingleOutputRetrier
import com.pittscraft.kotlinretrier.policybuilding.giveUpAfterTimeout
import com.pittscraft.kotlinretrier.policybuilding.giveUpOn
import com.pittscraft.kotlinretrier.retriers.ConditionalRetrier
import com.pittscraft.kotlinretrier.retriers.SimpleRetrier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Duration

class ColdRetrier(private val policy: RetryPolicy, private val conditionFlow: Flow<Boolean>?) {

    fun giveUpOn(criterium: (AttemptFailure<*>) -> Boolean): ColdRetrier {
        val policy = this.policy.giveUpOn(criterium)
        return ColdRetrier(policy, conditionFlow)
    }

    fun giveUpAfterMaxAttempts(maxAttempts: UInt): ColdRetrier {
        return giveUpOn { it.index >= maxAttempts - 1u }
    }

    fun giveUpAfterTimeout(timeout: Duration): ColdRetrier {
        val policy = this.policy.giveUpAfterTimeout(timeout)
        return ColdRetrier(policy, conditionFlow)
    }

    fun giveUpOnErrorsMatching(finalErrorCriterium: (Throwable) -> Boolean): ColdRetrier {
        return giveUpOn { finalErrorCriterium(it.error) }
    }

    fun onlyWhen(conditionFlow: Flow<Boolean>): ColdRetrier {
        var flow = conditionFlow
        if (this.conditionFlow != null) {
            flow = this.conditionFlow.combine(conditionFlow) { firstCondition, secondCondition ->
                firstCondition && secondCondition
            }
        }
        return ColdRetrier(policy, flow)
    }

    fun repeatingWithDelay(delay: Duration): ColdRepeater {
        return ColdRepeater(policy, delay, conditionFlow)
    }

    fun <Output>task(task: suspend () -> Output): SingleOutputRetrier<Output> {
        return conditionFlow?.let {
            ConditionalRetrier(policy, it, task)
        } ?: SimpleRetrier(policy, task)
    }
}