package com.blueprint.cubing.core.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform

fun <T> Flow<T>.shareSuspendingWhileNoSubs(
    scope: CoroutineScope,
    timeoutMillis: Long = 5_000,
    bufferCapacity: Int = 50,
    replay: Int = 0,
    replayExpirationMillis: Long = Long.MAX_VALUE
): SharedFlow<T> {
    var subsCount: StateFlow<Int>? = null
    val shareWhileSubscribed = SharingStarted.WhileSubscribed(
        stopTimeoutMillis = timeoutMillis,
        replayExpirationMillis = replayExpirationMillis
    )

    val sharingStarted = object : SharingStarted {
        override fun command(subscriptionCount: StateFlow<Int>): Flow<SharingCommand> {
            subsCount = subscriptionCount
            return shareWhileSubscribed.command(subscriptionCount)
        }
    }

    return this
        .buffer(bufferCapacity)
        .transform {
            subsCount?.first { count -> count > 0 }
            emit(it)
        }
        .shareIn(scope, sharingStarted, replay)
}
