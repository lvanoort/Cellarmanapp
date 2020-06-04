package com.lukevanoort.cellarman.app.canvases

import android.app.Activity
import androidx.annotation.UiThread
import com.lukevanoort.cellarman.app.R
import com.lukevanoort.cellarman.tasks.RootCanvas
import com.lukevanoort.cellarman.tasks.RootTaskState
import com.lukevanoort.cellarman.tasks.RootTaskViewModel
import com.lukevanoort.stuntman.SMBackPressedListener
import com.lukevanoort.stuntman.SMCanvas
import com.lukevanoort.stuntman.SMTask
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class RootCanvasFactory @Inject constructor(
    private val createVesselCanvasFactory: CreateVesselCanvasFactory,
    private val homeScreenCanvasFactory: HomeScreenCanvasFactory,
    private val viewVesselCanvasFactory: ManageVesselCanvasFactory,
    private val createSampleCanvasFactory: CreateSampleCanvasFactory,
    private val createFillCanvasFactory: CreateFillCanvasFactory
) {
    fun createRootCanvas(act:Activity) : RootCanvas {
        return RootCanvasImpl(
            act,
            createVesselCanvasFactory,
            homeScreenCanvasFactory,
            viewVesselCanvasFactory,
            createSampleCanvasFactory,
            createFillCanvasFactory
        )
    }
}

class RootCanvasImpl(
    private val act: Activity,
    private val createVesselFactory: CreateVesselCanvasFactory,
    private val homeScreenCanvasFactory: HomeScreenCanvasFactory,
    private val viewVesselCanvasFactory: ManageVesselCanvasFactory,
    private val createSampleCanvasFactory: CreateSampleCanvasFactory,
    private val createFillCanvasFactory: CreateFillCanvasFactory
) : RootCanvas, SMBackPressedListener {
    private var currentVm: RootTaskViewModel? = null
    private var disposable: Disposable? = null
    private val vmSyncLock: Any = Any()

    private var nextState: RootTaskState? = null
    private var currentState: RootTaskState? = null
    private var stateLock: Any = Any()

    private var currentSubtaskCanvasPair: Pair<SMCanvas, SMTask<*, *>>? = null

    @UiThread
    private fun reset() {
        val localCurrent = currentState
        val localNext = nextState
        if (localNext != localCurrent) {
            currentSubtaskCanvasPair?.first?.detachAllViewModels()

            when(localNext) {
                null -> {
                    act.setContentView(R.layout.blank)
                    currentSubtaskCanvasPair = null
                }
                is RootTaskState.HomeScreen -> {
                    val canvas = homeScreenCanvasFactory.createCanvasInActivity(act)
                    localNext.homeScreenTask.useCanvas(canvas)
                    currentSubtaskCanvasPair = Pair(canvas,localNext.homeScreenTask)
                }
                is RootTaskState.CreatingVessel -> {
                    val canvas = createVesselFactory.createCanvasInActivity(act)
                    localNext.createVesselTask.useCanvas(canvas)
                    currentSubtaskCanvasPair = Pair(canvas,localNext.createVesselTask)
                }
                is RootTaskState.ViewingVessel -> {
                    val canvas = viewVesselCanvasFactory.createCanvasInActivity(act)
                    localNext.viewingVesselTask.useCanvas(canvas)
                    currentSubtaskCanvasPair = Pair(canvas,localNext.viewingVesselTask)

                }
                is RootTaskState.CreatingSample -> {
                    val canvas = createSampleCanvasFactory.createCanvasInActivity(act)
                    localNext.createSampleTask.useCanvas(canvas)
                    currentSubtaskCanvasPair = Pair(canvas,localNext.createSampleTask)

                }
                is RootTaskState.CreatingFill -> {
                    val canvas = createFillCanvasFactory.createCanvasInActivity(act)
                    localNext.createFillTask.useCanvas(canvas)
                    currentSubtaskCanvasPair = Pair(canvas,localNext.createFillTask)

                }
            }
            currentState = localNext
        }
    }

    override fun attachRootTaskViewModel(vm: RootTaskViewModel) {
        synchronized(vmSyncLock) {
            disposable?.dispose()
            currentVm = vm
            disposable = vm.getState().subscribe {
                nextState = it
                act.runOnUiThread {
                    reset()
                }
            }
        }
    }

    override fun detachAllViewModels() {
        synchronized(vmSyncLock) {
            disposable?.dispose()
            currentVm = null
            currentSubtaskCanvasPair?.first?.detachAllViewModels()
        }
    }

    override fun backPressed(): Boolean {
        val localCurrentCanvas = currentSubtaskCanvasPair?.first
        return if (localCurrentCanvas is SMBackPressedListener) {
            localCurrentCanvas.backPressed()
        } else {
            false
        }
    }

}