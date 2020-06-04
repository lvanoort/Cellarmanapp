package com.lukevanoort.stuntman

class SMVMBinder<VM: SMViewModel<Any, Any>, VT: SMView<*, VM>> {
    private val syncLock = Any()
    private var vm: VM? = null
    private var v: VT? = null

    fun attachView(view : VT) {
        synchronized(syncLock) {
            v?.unbindViewModel()

            val localVm = vm
            if (localVm != null) {
                view.bindViewModel(localVm)
            }

            v = view
        }
    }

    fun detachView() {
        synchronized(syncLock) {
            v?.unbindViewModel()
        }
    }

    fun bindVM(viewModel : VM) {
        synchronized(syncLock) {
            v?.unbindViewModel()
            v?.bindViewModel(viewModel)

            vm = viewModel
        }
    }

    fun unbindVM() {
        synchronized(syncLock) {
            v?.unbindViewModel()
            vm = null
        }
    }
}