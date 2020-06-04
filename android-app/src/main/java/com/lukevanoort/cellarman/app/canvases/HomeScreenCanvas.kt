package com.lukevanoort.cellarman.app.canvases

import android.app.Activity
import android.view.View
import com.lukevanoort.cellarman.app.R
import com.lukevanoort.cellarman.ui.common.android.SMButton
import com.lukevanoort.stuntman.SMView
import com.lukevanoort.cellarman.tasks.HomeScreenCanvas
import com.lukevanoort.cellarman.vessel.logic.VesselListViewModel
import com.lukevanoort.cellarman.vessel.logic.VesselListViewState
import com.lukevanoort.stuntman.SimpleButtonState
import com.lukevanoort.stuntman.SimpleButtonVM
import javax.inject.Inject

private val LAYOUT_ID: Int = R.layout.home_canvas

class HomeScreenCanvasFactory @Inject constructor() {
    fun createCanvasInActivity(act:Activity) : HomeScreenCanvas {
        act.setContentView(LAYOUT_ID)
        return HomeScreenCanvasImpl(
            act.findViewById<View>(
                R.id.home_canvas
            )
        )
    }
}

class HomeScreenCanvasImpl constructor(val vesselView: View) : HomeScreenCanvas {
    private val vmSyncLock: Any = Any()

    private val vesselListView: SMView<VesselListViewState, VesselListViewModel> = vesselView.findViewById<com.lukevanoort.cellarman.vessel.ui.android.VesselListView>(R.id.vlv_vessel_list)
    private val addButton: SMView<SimpleButtonState, SimpleButtonVM> = vesselView.findViewById<SMButton>(R.id.smbt_add_vessel)


    override fun attachVesselListViewModel(vm: VesselListViewModel) {
        synchronized(vmSyncLock) {
            vesselListView.unbindViewModel()
            vesselListView.bindViewModel(vm)
        }
    }


    override fun attachCreateVesselRequestViewModel(vm: SimpleButtonVM) {
        synchronized(vmSyncLock) {
            addButton.unbindViewModel()
            addButton.bindViewModel(vm)
        }
    }

    override fun detachAllViewModels() {
        synchronized(vmSyncLock) {
            addButton.unbindViewModel()
            vesselListView.unbindViewModel()
        }
    }

}