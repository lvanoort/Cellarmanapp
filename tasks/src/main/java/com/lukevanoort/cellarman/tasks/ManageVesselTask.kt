package com.lukevanoort.cellarman.tasks

import androidx.annotation.UiThread
import com.lukevanoort.cellarman.fill.logic.FillListOutEvent
import com.lukevanoort.cellarman.fill.logic.FillListViewModel
import com.lukevanoort.cellarman.fill.logic.FillListViewModelFactory
import com.lukevanoort.cellarman.sample.logic.SampleListOutEvent
import com.lukevanoort.cellarman.sample.logic.SampleListViewModel
import com.lukevanoort.cellarman.sample.logic.SampleListViewModelFactory
import com.lukevanoort.cellarman.vessel.*
import com.lukevanoort.cellarman.vessel.logic.EditVesselOutEvent
import com.lukevanoort.cellarman.vessel.logic.EditVesselViewModel
import com.lukevanoort.cellarman.vessel.logic.EditVesselViewModelFactory
import com.lukevanoort.cellarman.vessel.model.Vessel
import com.lukevanoort.stuntman.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.lang.ref.WeakReference
import java.util.UUID
import javax.inject.Inject

sealed class ManageVesselOutput {
    data class DeletedVessel(val vessel: Vessel) : ManageVesselOutput()
    data class UpdatedVessel(val vessel: Vessel) : ManageVesselOutput()
    data class ViewSample(val sampleID: UUID) : ManageVesselOutput()
    data class ViewFill(val sampleID: UUID) : ManageVesselOutput()
    data class AddFill(val vesselID: UUID) : ManageVesselOutput()
    data class AddSample(val vesselID: UUID) : ManageVesselOutput()
    object Cancelled : ManageVesselOutput()
}

interface ManageVesselCanvas : SMCanvas {
    fun attachViewVesselViewModel(vm : EditVesselViewModel)
    fun attachFillListViewModel(vm : FillListViewModel)
    fun attachSampleListViewModel(vm : SampleListViewModel)

    fun attachBackButtonViewModel(vm : SimpleButtonVM)
    fun attachAddFillViewModel(vm: SimpleButtonVM)
    fun attachAddSampleViewModel(vm: SimpleButtonVM)
}

class ManageVesselTaskFactory @Inject constructor(
    private val viewVesselVMFactory: EditVesselViewModelFactory,
    private val fillListViewModelFactory: FillListViewModelFactory,
    private val sampleListViewModelFactory: SampleListViewModelFactory,
    private val simpleButtonVMFactory: SimpleButtonVMFactory
) {
    fun getViewVesselTask(vesselID: UUID, wad: SMTaskReadableDataWad?) : ManageVesselTask {
        return ManageVesselTask(
            vesselID,
            viewVesselVMFactory,
            fillListViewModelFactory,
            sampleListViewModelFactory,
            simpleButtonVMFactory
        )
    }
}

class ManageVesselTask constructor(
    vesselID: UUID,
    viewVesselVMFactory: EditVesselViewModelFactory,
    fillListViewModelFactory: FillListViewModelFactory,
    sampleListViewModelFactory: SampleListViewModelFactory,
    simpleButtonVMFactory: SimpleButtonVMFactory
) : SMTask<ManageVesselCanvas, ManageVesselOutput> {
    private var canvasRef: WeakReference<ManageVesselCanvas?> = WeakReference(null)

    private val eventObs: PublishSubject<ManageVesselOutput> = PublishSubject.create()

    private val subs = CompositeDisposable()

    private val cvVm = viewVesselVMFactory.getVesselViewModel(vesselID)
    private val backVM = simpleButtonVMFactory.getAlwaysEnabledButtonViewModel()
    private val fillVm = fillListViewModelFactory.getVesselFillListViewModel(vesselID)
    private val sampleVm = sampleListViewModelFactory.getVesselSampleListViewModel(vesselID)

    private val addFillVm = simpleButtonVMFactory.getAlwaysEnabledButtonViewModel()
    private val addSampleVm = simpleButtonVMFactory.getAlwaysEnabledButtonViewModel()

    init {
        cvVm.getOutput().subscribe { o ->
            when (o) {
                is EditVesselOutEvent.VesselDeleted -> ManageVesselOutput.DeletedVessel(o.vessel)
                is EditVesselOutEvent.VesselDeleteFailed -> null
                is EditVesselOutEvent.VesselUpdated -> ManageVesselOutput.UpdatedVessel(o.newData)
                is EditVesselOutEvent.VesselUpdateFailed -> null
                is EditVesselOutEvent.Cancel -> ManageVesselOutput.Cancelled
            }?.let {
                eventObs.onNext(it)
            }
        }.also { subs.add(it) }

        backVM.getOutput().subscribe { o ->
            when(o) {
                SimpleButtonOutputEvents.Pressed -> ManageVesselOutput.Cancelled
            }.let {
                eventObs.onNext(it)
            }
        }.also { subs.add(it) }

        fillVm.getOutput().subscribe {o ->
            when(o) {
                is FillListOutEvent.FillSelected -> ManageVesselOutput.ViewFill(o.id)
            }?.let {
                eventObs.onNext(it)
            }
        }.also { subs.add(it) }

        sampleVm.getOutput().subscribe { o ->
            when(o) {
                is SampleListOutEvent.SampleSelected -> ManageVesselOutput.ViewSample(o.id)
            }?.let {
                eventObs.onNext(it)
            }
        }.also { subs.add(it) }

        addFillVm.getOutput().subscribe { o ->
            when(o) {
                SimpleButtonOutputEvents.Pressed -> ManageVesselOutput.AddFill(vesselID)
            }.let {
                eventObs.onNext(it)
            }
        }.also { subs.add(it) }

        addSampleVm.getOutput().subscribe { o ->
            when(o) {
                SimpleButtonOutputEvents.Pressed -> ManageVesselOutput.AddSample(vesselID)
            }.let {
                eventObs.onNext(it)
            }
        }.also { subs.add(it) }
    }


    @UiThread
    override fun useCanvas(c: ManageVesselCanvas) {
        removeCanvas()
        c.attachViewVesselViewModel(cvVm)
        c.attachBackButtonViewModel(backVM)
        c.attachFillListViewModel(fillVm)
        c.attachSampleListViewModel(sampleVm)
        c.attachAddFillViewModel(addFillVm)
        c.attachAddSampleViewModel(addSampleVm)
        canvasRef = WeakReference(c)
    }

    @UiThread
    override fun removeCanvas() {
        canvasRef.get()?.detachAllViewModels()
        canvasRef = WeakReference(null)
    }

    override fun finished() {
        removeCanvas()
        subs.clear()
        eventObs.onComplete()
    }

    override fun getOutput(): Observable<ManageVesselOutput> = eventObs
}