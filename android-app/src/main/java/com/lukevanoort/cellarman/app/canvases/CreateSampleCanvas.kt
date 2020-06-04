package com.lukevanoort.cellarman.app.canvases

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import com.lukevanoort.cellarman.app.R
import com.lukevanoort.cellarman.sample.logic.SampleTypeChooserViewModel
import com.lukevanoort.cellarman.tasks.CreateSampleCanvas
import com.lukevanoort.cellarman.tasks.CreatingSampleVMState
import com.lukevanoort.cellarman.tasks.CreatingSampleViewModel
import com.lukevanoort.stuntman.SMBackPressedListener
import io.reactivex.disposables.Disposable
import javax.inject.Inject

private const val LAYOUT_ID = R.layout.create_sample_canvas

class CreateSampleCanvasFactory @Inject constructor() {
    fun createCanvasInActivity(act:Activity) : CreateSampleCanvas {
        act.setContentView(LAYOUT_ID)
        return CreateSampleCanvasImpl(
            act.findViewById<View>(
                R.id.create_sample_canvas
            )
        )
    }
}

class CreateSampleCanvasImpl constructor(val sampleView: View) : CreateSampleCanvas,
    SMBackPressedListener {
    private val vmSyncLock: Any = Any()

    private var createVm : CreatingSampleViewModel? = null
    private var createVmSub : Disposable? = null

    private var canceller : (() -> Unit)?  = null

    val sampleTypeChooserView: com.lukevanoort.sample.ui.android.SampleTypeChooserView = sampleView.findViewById<com.lukevanoort.sample.ui.android.SampleTypeChooserView>(R.id.stcv_sample_type)

    val contentHolder: NestedScrollView = sampleView.findViewById<NestedScrollView>(R.id.nsv_content_holder)

    val genericSampleView = com.lukevanoort.sample.ui.android.GenericSampleDataView(
        contentHolder.context
    ).also {
        it.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    val beerSampleView = com.lukevanoort.sample.ui.android.BeerSampleDataView(
        contentHolder.context
    ).also {
        it.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }



    override fun attachSampleViewModel(vm: CreatingSampleViewModel) {
        synchronized(vmSyncLock) {
            createVm = vm
            createVmSub = vm.getState().subscribe { newState ->
                when(newState) {
                    is CreatingSampleVMState.CreatingBeerSample -> {
                        genericSampleView.unbindViewModel()
                        contentHolder.removeAllViews()
                        contentHolder.addView(beerSampleView)
                        canceller = {
                            newState.vm.cancel()
                        }
                        beerSampleView.also { v->
                            v.bindViewModel(newState.vm)
                        }
                    }
                    is CreatingSampleVMState.CreatingGenericSample -> {
                        beerSampleView.unbindViewModel()
                        contentHolder.removeAllViews()
                        contentHolder.addView(genericSampleView)
                        canceller = {
                            newState.vm.cancel()
                        }
                        genericSampleView.also { v->
                            v.bindViewModel(newState.vm)
                        }
                    }
                }
            }
        }
    }

    override fun attachSampleTypeViewModel(vm: SampleTypeChooserViewModel) {
        synchronized(vmSyncLock) {
            sampleTypeChooserView.bindViewModel(vm)
        }
    }


    override fun detachAllViewModels() {
        synchronized(vmSyncLock) {
            genericSampleView.unbindViewModel()
            beerSampleView.unbindViewModel()
            createVmSub?.dispose()
            createVm = null
            canceller = null
            sampleTypeChooserView.unbindViewModel()
        }
    }

    override fun backPressed(): Boolean {
        canceller?.let { it() }
        return true
    }

}