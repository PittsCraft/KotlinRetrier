package com.pittscraft.kotlinretrier

import com.pittscraft.kotlinretrier.abstracttests.RetrierTests
import com.pittscraft.kotlinretrier.abstracttests.SingleOutputRetrierTests
import com.pittscraft.kotlinretrier.retriers.ConditionalRetrier
import kotlinx.coroutines.flow.flowOf

class RetrierTestsForConditionalRetrier: RetrierTests<ConditionalRetrier<Unit>>(
    retrierBuilder =  { policy, task -> ConditionalRetrier(policy, flowOf(true), task) }
)

class SingleOutputRetrierTestsForConditionalRetrier: SingleOutputRetrierTests<ConditionalRetrier<Unit>>(
    retrierBuilder =  { policy, task -> ConditionalRetrier(policy, flowOf(true), task) }
)
