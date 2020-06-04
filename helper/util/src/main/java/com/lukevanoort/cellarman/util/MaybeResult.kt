package com.lukevanoort.cellarman.util

sealed class MaybeResult<out T : Any> {
    data class HasResult<out T : Any>(val result: T) : MaybeResult<T>()
    class NoResult<out T : Any> : MaybeResult<T>()
}

fun <T: Any> MaybeResult<T>.asNullable() : T? = when(this) {
    is MaybeResult.HasResult -> this.result
    is MaybeResult.NoResult -> null
}

fun <T: Any> MaybeResult<T>.asDefault(default : T) : MaybeResult.HasResult<T> = when(this) {
    is MaybeResult.HasResult -> this
    is MaybeResult.NoResult -> MaybeResult.HasResult<T>(default)
}