package com.lukevanoort.cellarman.tasks

import androidx.annotation.UiThread
import com.lukevanoort.cellarman.vessel.logic.VesselListOutEvent
import com.lukevanoort.cellarman.vessel.logic.VesselListViewModel
import com.lukevanoort.cellarman.vessel.logic.VesselListViewModelFactory
import com.lukevanoort.stuntman.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.lang.ref.WeakReference
import java.util.UUID
import javax.inject.Inject

sealed class HomeScreenOutput {
    data class RequestedViewVessel(val id: UUID) : HomeScreenOutput()
    object RequestedCreateVessel : HomeScreenOutput()
}

interface HomeScreenCanvas : SMCanvas {
    fun attachVesselListViewModel(vm : VesselListViewModel)
    fun attachCreateVesselRequestViewModel(vm: SimpleButtonVM)
}

class HomeScreenTaskFactory @Inject constructor(
    val vesselListVMFactory: VesselListViewModelFactory,
    val buttonVMFactory: SimpleButtonVMFactory
)  {
    fun getViewVesselTask(wad: SMTaskReadableDataWad?) : HomeScreenTask {
        return HomeScreenTask(
            vesselListVMFactory,
            buttonVMFactory
        )
    }
}

class HomeScreenTask constructor(
    vesselListVMFactory: VesselListViewModelFactory,
    buttonVMFactory: SimpleButtonVMFactory
) : SMTask<HomeScreenCanvas, HomeScreenOutput> {
    private var canvasRef: WeakReference<HomeScreenCanvas?> = WeakReference(null)

    private val eventObs: PublishSubject<HomeScreenOutput> = PublishSubject.create()
    private val vmSubs: CompositeDisposable = CompositeDisposable()


    private val vlVm = vesselListVMFactory.getVesselListViewModel()
    private val cvVm = buttonVMFactory.getAlwaysEnabledButtonViewModel()

    init {
        vlVm.getOutput().subscribe {
            when (it) {
                is VesselListOutEvent.VesselSelected -> {
                    eventObs.onNext(HomeScreenOutput.RequestedViewVessel(it.id))
                }
            }
        }.also { vmSubs.add(it) }
        cvVm.getOutput().subscribe {
            eventObs.onNext(HomeScreenOutput.RequestedCreateVessel)
        }.also { vmSubs.add(it) }
    }

    @UiThread
    override fun useCanvas(c: HomeScreenCanvas) {
        removeCanvas()
        c.attachVesselListViewModel(vlVm)
        c.attachCreateVesselRequestViewModel(cvVm)
        canvasRef = WeakReference(c)
    }

    @UiThread
    override fun removeCanvas() {
        canvasRef.get()?.detachAllViewModels()
        canvasRef = WeakReference(null)
    }

    override fun finished() {
        removeCanvas()
        vmSubs.clear()
    }

    override fun getOutput(): Observable<HomeScreenOutput> = eventObs

}