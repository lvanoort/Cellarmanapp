package com.lukevanoort.stuntman

import com.jakewharton.rxrelay2.PublishRelay
import com.lukevanoort.stuntman.SMViewModel
import io.reactivex.Observable
import javax.inject.Inject

sealed class SimpleButtonState {
    object Enabled : SimpleButtonState()
    object Disabled : SimpleButtonState()
}

sealed class SimpleButtonOutputEvents {
    object Pressed : SimpleButtonOutputEvents()
}

interface SimpleButtonVM:
    SMViewModel<SimpleButtonState, SimpleButtonOutputEvents> {
    fun buttonPressed()
}

class SimpleButtonVMFactory @Inject constructor() {

    fun getAlwaysEnabledButtonViewModel() : SimpleButtonVM {
        return SimpleButtonVMImpl(
            Observable.just(true)
        )
    }
}

class SimpleButtonVMImpl constructor(
    private val enabledObs : Observable<Boolean>
) : SimpleButtonVM {
    private val relay : PublishRelay<SimpleButtonOutputEvents> = PublishRelay.create()

    override fun buttonPressed() {
        relay.accept(SimpleButtonOutputEvents.Pressed)
    }

    override fun getState(): Observable<SimpleButtonState> = enabledObs.map {
        if (it) {
            SimpleButtonState.Enabled
        } else {
            SimpleButtonState.Disabled
        }
    }
    override fun getOutput(): Observable<SimpleButtonOutputEvents> = relay

}

