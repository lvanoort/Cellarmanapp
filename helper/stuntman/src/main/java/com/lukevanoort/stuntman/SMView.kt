package com.lukevanoort.stuntman


interface SMView<S: Any, T : SMViewModel<S, Any>> {
    abstract fun bindViewModel(vm : T)
    abstract fun setState(state: S)
    abstract  fun unbindViewModel()
}
