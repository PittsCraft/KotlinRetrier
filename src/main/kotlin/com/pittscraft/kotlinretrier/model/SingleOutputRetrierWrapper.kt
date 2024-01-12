package com.pittscraft.kotlinretrier.model

import kotlinx.coroutines.flow.FlowCollector

internal class SingleOutputRetrierWrapper<Output>(private val retrier: Retrier<Output>): SingleOutputRetrier<Output> {
    override suspend fun collect(collector: FlowCollector<RetrierEvent<Output>>) {
        retrier.collect(collector)
    }
}

fun <Output>Retrier<Output>.singleOutput(): SingleOutputRetrier<Output> {
    return SingleOutputRetrierWrapper(this)
}