package com.pittscraft.kotlinretrier.abstracttests

import com.pittscraft.kotlinretrier.Expectation
import com.pittscraft.kotlinretrier.model.*
import com.pittscraft.kotlinretrier.policies.ConstantDelayRetryPolicy
import com.pittscraft.kotlinretrier.wait
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds

abstract class RetrierTests<R : Retrier<Unit>>(private val retrierBuilder: (RetryPolicy, suspend () -> Unit) -> R) {

    @Test
    fun `test emit nothing after cancel`() = runTest {
        val retrier = retrierBuilder(ConstantDelayRetryPolicy()) { throw Error() }
        var cancelled = false
        val job = launch {
            retrier
                .collect {
                    if (cancelled) {
                        fail("Didn't expect any event after cancellation")
                    }
                }
        }
        wait(100.milliseconds)
        job.cancel()
        cancelled = true
        wait(100.milliseconds)
    }

    @Test
    fun `test coroutines complete after cancel`() = runTest(timeout = 200.milliseconds) {
        val retrier = retrierBuilder(ConstantDelayRetryPolicy()) { throw Error() }
        val job = launch { retrier.collect() }
        job.cancel()
        // Test will hang and expire if any subsequent job is still running
    }

    @Test
    fun `test collect success`() = runTest {
        val retrier = retrierBuilder(ConstantDelayRetryPolicy()) {}
        val gotSuccess = Expectation("Got success")
        val job = launch {
            retrier
                .takeWhile {
                    !gotSuccess.isFulfilled
                }
                .collect {
                    if (it is AttemptSuccess) {
                        gotSuccess.fulfill()
                    }
                }
        }
        gotSuccess.awaitFulfillment(joining = job)
    }

    @Test
    fun `test collect attempt failure`() = runTest {
        val gotFailure = Expectation("Got failure", allowMultipleFulfillments = true)
        val retrier = retrierBuilder(ConstantDelayRetryPolicy()) { throw Error() }
        val job = launch {
            retrier
                .collect {
                    if (it is AttemptFailure) {
                        gotFailure.fulfill()
                    }
                }
        }
        gotFailure.awaitFulfillment()
        job.cancel()
    }

    @Test
    fun `test retries`() = runTest {
        var calledOnce = false
        val retrier = retrierBuilder(ConstantDelayRetryPolicy(5.milliseconds)) {
            if (!calledOnce) {
                calledOnce = true
                throw Error()
            }
        }
        var gotFailure = false
        val gotSuccess = Expectation("Got success")
        val job = launch {
            retrier
                .takeWhile { !gotSuccess.isFulfilled }
                .collect {
                    println(it)
                    when (it) {
                        is AttemptFailure -> gotFailure = true
                        is AttemptSuccess -> {
                            if (!gotFailure) {
                                fail("Should have received failure before success")
                            }
                            gotSuccess.fulfill()
                        }

                        is Completion -> {}
                    }
                }
        }
        gotSuccess.awaitFulfillment(joining = job)
    }
}