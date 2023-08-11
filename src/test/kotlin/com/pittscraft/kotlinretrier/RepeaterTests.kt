package com.pittscraft.kotlinretrier

import com.pittscraft.kotlinretrier.abstracttests.RetrierTests
import com.pittscraft.kotlinretrier.retriers.Repeater
import kotlin.time.Duration.Companion.milliseconds

class RetrierTestsForRepeater: RetrierTests<Repeater<Unit>>(
    retrierBuilder = { policy, task -> Repeater(policy, 50.milliseconds, task) }
)
