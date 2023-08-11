package com.pittscraft.kotlinretrier

import com.pittscraft.kotlinretrier.abstracttests.RetrierTests
import com.pittscraft.kotlinretrier.abstracttests.SingleOutputRetrierTests
import com.pittscraft.kotlinretrier.retriers.SimpleRetrier

class RetrierTestsForSimpleRetrier: RetrierTests<SimpleRetrier<Unit>>(
    retrierBuilder =  { policy, task -> SimpleRetrier(policy, task) }
)

class SingleOutputRetrierTestsForSimpleRetrier: SingleOutputRetrierTests<SimpleRetrier<Unit>>(
    retrierBuilder =  { policy, task -> SimpleRetrier(policy, task) }
)
