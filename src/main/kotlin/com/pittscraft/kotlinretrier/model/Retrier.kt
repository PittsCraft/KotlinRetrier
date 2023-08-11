package com.pittscraft.kotlinretrier.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

typealias Retrier<Output> = Flow<RetrierEvent<Output>>

fun <Output>Retrier<Output>.values(): Flow<Output> {
    return mapNotNull { it.success }
        .map { it.value }
}