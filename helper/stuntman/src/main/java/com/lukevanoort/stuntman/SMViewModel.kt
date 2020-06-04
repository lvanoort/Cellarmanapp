package com.lukevanoort.stuntman

import io.reactivex.Observable

interface SMViewModel<out State : Any, out OutEvent: Any> {
    fun getState() : Observable<out State>
    fun getOutput() : Observable<out OutEvent>
}