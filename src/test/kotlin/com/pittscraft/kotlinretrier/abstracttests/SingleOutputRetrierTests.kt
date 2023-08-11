package com.pittscraft.kotlinretrier.abstracttests

import com.pittscraft.kotlinretrier.Expectation
import com.pittscraft.kotlinretrier.model.*
import com.pittscraft.kotlinretrier.policies.ConstantDelayRetryPolicy
import com.pittscraft.kotlinretrier.retriers.SimpleRetrier
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.fail

abstract class SingleOutputRetrierTests <R: SingleOutputRetrier<Unit>>(
    private val retrierBuilder: (RetryPolicy, suspend () -> Unit) -> R
) {
    @Test
    fun `test collect success then completion`() = runTest {
        var gotSuccess = false
        val gotCompletion = Expectation("Got completion")
        val retrier = SimpleRetrier(ConstantDelayRetryPolicy()) {}
        launch {
            retrier
                .collect {
                    when (it) {
                        is AttemptFailure -> fail("Unexpected failure")
                        is AttemptSuccess ->
                            gotSuccess = true
                        is Completion -> {
                            if (!gotSuccess) {
                                fail("Expected success before completion")
                            }
                            gotCompletion.fulfill()
                        }
                    }
                }
        }
        gotCompletion.awaitFulfillment()
    }
}