package com.lukevanoort.cellarman.vessel.logic

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.lukevanoort.cellarman.dagger.IOScheduler
import com.lukevanoort.stuntman.SMViewModel
import com.lukevanoort.cellarman.units.VolumeMeasurement
import com.lukevanoort.cellarman.units.VolumeUnit
import com.lukevanoort.cellarman.util.SuccessIndicator
import com.lukevanoort.cellarman.vessel.model.Vessel
import com.lukevanoort.cellarman.vessel.model.VesselData
import com.lukevanoort.cellarman.vessel.model.VesselMaterial
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.functions.Consumer
import java.lang.Exception
import java.util.UUID
import javax.inject.Inject

sealed class CreateVesselViewState {
    data class CreatingVessel(val vessel: Vessel): CreateVesselViewState()
}

sealed class CreateVesselVMOutputs {
    data class VesselCreated(
        val vessel: Vessel
    ) : CreateVesselVMOutputs()

    data class VesselCreationFailed(
        val vessel: Vessel,
        val reason: Exception
    ) : CreateVesselVMOutputs()
    object CancelRequested : CreateVesselVMOutputs()
}

interface CreateVesselViewModel :
    SMViewModel<CreateVesselViewState, CreateVesselVMOutputs> {
    override fun getState() : Observable<CreateVesselViewState>
    override fun getOutput(): Observable<CreateVesselVMOutputs>
    fun updateVessel(updatedVessel: Vessel)
    fun createVessel(vesselToCreate: Vessel)
    fun cancel()
}



class CreateVesselViewModelFactory @Inject constructor(
    private val repository: VesselRepository,
    @IOScheduler private val ioSched: Scheduler
) {

    fun getVesselViewViewModelFromTemp(
        tempStateConsumer: Consumer<Vessel>,
        tempStateSource: Observable<Vessel>) : CreateVesselViewModel {
        return CreateVesselViewModelImpl(
            repository,
            ioSched,
            tempStateConsumer,
            tempStateSource
        )
    }

    fun getVesselViewViewModel() : CreateVesselViewModel {
        val relay = BehaviorRelay.createDefault(
            VesselData(
                id = UUID.randomUUID(),
                name = "",
                capacity = VolumeMeasurement(
                    18.7,
                    VolumeUnit.Liter
                ),
                material = VesselMaterial.StainlessSteel,
                notes = ""
            ) as Vessel
        )
        return getVesselViewViewModelFromTemp(
            relay,
            relay
        )
    }
}

class CreateVesselViewModelImpl (
    private val repo: VesselRepository,
    private val ioSched: Scheduler,
    private val tempStateConsumer: Consumer<Vessel>,
    private val tempStateSource: Observable<Vessel>
) : CreateVesselViewModel {
    private val eventRelay: PublishRelay<CreateVesselVMOutputs> = PublishRelay.create()

    override fun getState(): Observable<CreateVesselViewState> = tempStateSource.map {
        CreateVesselViewState.CreatingVessel(it)
    }

    override fun getOutput(): Observable<CreateVesselVMOutputs> = eventRelay

    override fun updateVessel(updatedVessel: Vessel) {
        tempStateConsumer.accept(updatedVessel)
    }

    override fun createVessel(vesselToCreate: Vessel) {
        Observable.just(vesselToCreate)
            .observeOn(ioSched)
            .map {
                Pair(it,repo.addVessel(it))
            }
            .subscribe {
                when(val res = it.second) {
                    com.lukevanoort.cellarman.util.SuccessIndicator.Success -> {
                        eventRelay.accept(CreateVesselVMOutputs.VesselCreated(it.first))
                    }
                    is com.lukevanoort.cellarman.util.SuccessIndicator.Failure -> {
                        eventRelay.accept(CreateVesselVMOutputs.VesselCreationFailed(it.first,res.reason))
                    }

                }
            }
    }

    override fun cancel() {
        eventRelay.accept(CreateVesselVMOutputs.CancelRequested)
    }

}