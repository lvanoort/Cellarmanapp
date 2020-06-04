package com.lukevanoort.cellarman.app.canvases

import android.app.Activity
import android.view.View
import com.lukevanoort.cellarman.app.R
import com.lukevanoort.cellarman.fill.logic.CreateFillViewModel
import com.lukevanoort.cellarman.fill.logic.CreateFillViewState
import com.lukevanoort.cellarman.fill.ui.android.CreateFillView
import com.lukevanoort.cellarman.tasks.CreateFillCanvas
import com.lukevanoort.stuntman.SMBackPressedListener
import com.lukevanoort.stuntman.SMView
import javax.inject.Inject

private const val LAYOUT_ID = R.layout.create_fill_canvas

class CreateFillCanvasFactory @Inject constructor() {
    fun createCanvasInActivity(act:Activity) : CreateFillCanvas {
        act.setContentView(LAYOUT_ID)
        return CreateFillCanvasImpl(
            act.findViewById<View>(
                R.id.create_fill_canvas
            )
        )
    }
}

class CreateFillCanvasImpl constructor(val rootView: View) : CreateFillCanvas,
    SMBackPressedListener {
    private val vmSyncLock: Any = Any()

//    private var createVm : CreateFillViewModel? = null
//    private var createVmSub : Disposable? = null
//
    private var canceller : (() -> Unit)?  = null

    val fillView: SMView<CreateFillViewState, CreateFillViewModel> = rootView.findViewById<CreateFillView>(R.id.cfv_fill_creator)

//
//    private var backVm: SimpleButtonVM? = null
//    private val backBt: SMView<SimpleButtonState, SimpleButtonVM> = rootView.findViewById<SMButton>(R.id.smb_goback)

    override fun attachCreateFillViewModel(vm: CreateFillViewModel) {
        synchronized(vmSyncLock) {
            fillView.bindViewModel(vm)
//            createVm = vm
//
            canceller = {
                vm.cancel()
            }
        }
    }

//    override fun attachFillTypeViewModel(vm: FillTypeChooserViewModel) {
//        synchronized(vmSyncLock) {
//            fillTypeChooserView.bindViewModel(vm)
//        }
//    }


    override fun detachAllViewModels() {
        synchronized(vmSyncLock) {
            fillView.unbindViewModel()
        }
    }

    override fun backPressed(): Boolean {
        canceller?.let { it() }
        return true
    }

}