package com.lukevanoort.cellarman.util

import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable

/** TransientStackManager is a largely threadsafe stack manager,
 * although it can theoretically end up disagreeing with itself
 * in terms of what order the the elements were added
 */
class TransientStackManager<T : Any> constructor(
    initialStack: List<T>? = null
) : HistoricalStackManager<T> {
    private val stack : ArrayList<T> = ArrayList(initialStack?.size ?: 1)
    private val lock = Any()
    private val latestValueStackRelay: BehaviorRelay<MaybeResult<T>> = BehaviorRelay.createDefault(
        MaybeResult.NoResult<T>())

    init {
        initialStack?.let {
            if (it.isNotEmpty()) {
                stack.addAll(initialStack)
                latestValueStackRelay.accept(MaybeResult.HasResult<T>(it.last()))
            }
        }
    }

    override fun getLatest(): Observable<MaybeResult<T>> = latestValueStackRelay

    override fun stackSize(): Int = synchronized(lock) {
        stack.size
    }

    private fun pump() {
        val toPump : MaybeResult<T> = synchronized(lock) {
            if (stack.isEmpty()) {
                MaybeResult.NoResult()
            } else {
                MaybeResult.HasResult(stack[stack.lastIndex])
            }
        }
        latestValueStackRelay.accept(toPump)
    }

    override fun push(next: T) {
        synchronized(lock) {
            stack.add(next)
        }
        pump()
    }

    override fun pop(): MaybeResult<T> {
        var pumpIt = false
        val result = synchronized(lock) {
            if (stack.isEmpty()) {
                MaybeResult.NoResult<T>()
            } else {
                pumpIt = true
                val r = stack.removeAt(stack.lastIndex)
                MaybeResult.HasResult<T>(r)
            }
        }

        if(pumpIt) {
            pump()
        }
        return result
    }

    override fun getStack(): List<T> {
        return synchronized(lock) {
            stack.toList()
        }
    }

}