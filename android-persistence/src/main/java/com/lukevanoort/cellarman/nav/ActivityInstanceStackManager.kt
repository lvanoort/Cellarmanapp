package com.lukevanoort.cellarman.nav

import android.os.Bundle
import com.jakewharton.rxrelay2.BehaviorRelay
import com.lukevanoort.cellarman.util.HistoricalStackManager
import com.lukevanoort.cellarman.util.MaybeResult
import io.reactivex.Observable
import javax.inject.Inject

interface StringConverter<T:Any> {
    fun fromString(str: String) : T?
    fun toString(value: T) : String
}

/** ActivityInstanceStackManager is a largely threadsafe stack manager,
 * although it can theoretically end up disagreeing with itself
 * in terms of what order the the elements were added. It supports persisting
 * to/from a bundle
 */
class ActivityInstanceStackManager<T : Any>  constructor(
    bundle: Bundle?,
    val toStringConverter: (T) -> String,
    val fromStringConverter: (String) -> T?
) : HistoricalStackManager<T> {
    private val stack : ArrayList<T> = ArrayList(1)

    init {
        bundle?.getStringArrayList(VALUES_KEY)?.forEach { strVal ->
            fromStringConverter(strVal)?.let { converted ->
                stack.add(converted)
            }
        }
    }

    private val lock = Any()
    private val latestValueStackRelay: BehaviorRelay<MaybeResult<T>> = BehaviorRelay.createDefault(
        MaybeResult.NoResult<T>())

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

    fun writeTo(bundle: Bundle) {
        var toPersist: ArrayList<String>
        synchronized(lock){
            toPersist = ArrayList(stack.size)
            stack.forEach { value ->
                toStringConverter(value).let { converted ->
                    toPersist.add(converted)
                }
            }
        }

        bundle.putStringArrayList(VALUES_KEY,toPersist)
    }

    companion object {
        private val VALUES_KEY = "STACK_VALUES"
    }

    override fun getStack(): List<T> {
        return synchronized(lock) {
            stack.toList()
        }
    }
}