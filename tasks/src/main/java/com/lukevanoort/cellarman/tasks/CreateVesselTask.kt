package com.lukevanoort.cellarman.tasks

import androidx.annotation.UiThread
import com.jakewharton.rxrelay2.BehaviorRelay
import com.lukevanoort.stuntman.SMCanvas
import com.lukevanoort.stuntman.SMTask
import com.lukevanoort.stuntman.SMTaskReadableDataWad
import com.lukevanoort.stuntman.SimpleButtonVMFactory
import com.lukevanoort.cellarman.units.VolumeMeasurement
import com.lukevanoort.cellarman.units.VolumeUnit
import com.lukevanoort.cellarman.vessel.*
import com.lukevanoort.cellarman.vessel.logic.CreateVesselVMOutputs
import com.lukevanoort.cellarman.vessel.logic.CreateVesselViewModel
import com.lukevanoort.cellarman.vessel.logic.CreateVesselViewModelFactory
import com.lukevanoort.cellarman.vessel.model.Vessel
import com.lukevanoort.cellarman.vessel.model.VesselData
import com.lukevanoort.cellarman.vessel.model.VesselMaterial
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.lang.ref.WeakReference
import java.util.UUID
import javax.inject.Inject

sealed class CreateVesselOutput {
    data class CreatedVessel(val vessel: Vessel) : CreateVesselOutput()
    object Cancelled : CreateVesselOutput()
}

interface CreateVesselCanvas : SMCanvas {
    fun attachCreateVesselViewModel(vm : CreateVesselViewModel)
}

class CreateVesselTaskFactory @Inject constructor(
    val createVesselVMFactory: CreateVesselViewModelFactory
)  {
    fun getViewVesselTask(wad: SMTaskReadableDataWad?) : CreateVesselTask {
        return CreateVesselTask(
            createVesselVMFactory
        )
    }
}

class CreateVesselTask constructor(
    createVesselVMFactory: CreateVesselViewModelFactory
) : SMTask<CreateVesselCanvas, CreateVesselOutput> {
    private var canvasRef: WeakReference<CreateVesselCanvas?> = WeakReference(null)

    private val eventObs: PublishSubject<CreateVesselOutput> = PublishSubject.create()

    private val tempRelay = BehaviorRelay.create<Vessel>()

    private val cvVm = createVesselVMFactory.getVesselViewViewModelFromTemp(tempRelay,tempRelay)

    private val vmSubs = CompositeDisposable()

    init {
        cvVm.getOutput().subscribe { o ->
            when(o) {
                is CreateVesselVMOutputs.VesselCreated -> {
                    eventObs.onNext(CreateVesselOutput.CreatedVessel(o.vessel))
                    resetVessel()
                }
                is CreateVesselVMOutputs.VesselCreationFailed -> {
                    //ignore
                }
                CreateVesselVMOutputs.CancelRequested -> {
                    eventObs.onNext(CreateVesselOutput.Cancelled)
                    resetVessel()
                }
            }
        }?.also { vmSubs.add(it) }
    }

    init {
        resetVessel()
    }

    private fun resetVessel() {
        tempRelay.accept(VesselData(
            id = UUID.randomUUID(),
            name = "",
            capacity = VolumeMeasurement(
                18.7,
                VolumeUnit.Liter
            ),
            material = VesselMaterial.StainlessSteel,
            notes = ""
        ) as Vessel)
    }

    @UiThread
    override fun useCanvas(c: CreateVesselCanvas) {
        removeCanvas()
        c.attachCreateVesselViewModel(cvVm)
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
        eventObs.onComplete()
    }

    override fun getOutput(): Observable<CreateVesselOutput> = eventObs



}