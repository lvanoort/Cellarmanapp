package com.lukevanoort.cellarman.util

import io.reactivex.Observable

interface HistoricalStackManager<T : Any> {
    fun getLatest() : Observable<MaybeResult<T>>
    fun stackSize() : Int
    fun getStack() : List<T>
    fun push(next : T)
    fun pop() : MaybeResult<T>
}


