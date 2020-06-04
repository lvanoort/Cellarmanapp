package com.lukevanoort.cellarman.vessel.logic

import com.jakewharton.rxrelay2.PublishRelay
import com.lukevanoort.cellarman.dagger.IOScheduler
import com.lukevanoort.stuntman.SMViewModel
import com.lukevanoort.cellarman.util.MaybeResult
import com.lukevanoort.cellarman.util.SuccessIndicator
import com.lukevanoort.cellarman.vessel.model.Vessel
import io.reactivex.Observable
import io.reactivex.Scheduler
import java.lang.Exception
import java.util.UUID
import javax.inject.Inject

sealed class EditVesselState {
    data class HasVessel(val vessel: Vessel) : EditVesselState()
    object NoVessel: EditVesselState()
}

sealed class EditVesselOutEvent {
    data class VesselDeleted(val vessel: Vessel) : EditVesselOutEvent()
    data class VesselDeleteFailed(val vessel: Vessel, val reason: Exception) : EditVesselOutEvent()
    data class VesselUpdated(val newData: Vessel) : EditVesselOutEvent()
    data class VesselUpdateFailed(val newData: Vessel, val reason : Exception): EditVesselOutEvent()
    object Cancel : EditVesselOutEvent()
}

interface EditVesselViewModel :
    SMViewModel<EditVesselState, EditVesselOutEvent> {
    fun updateVessel(newData: Vessel)
    fun deleteVessel(toDelete: Vessel)
    fun cancel()
}

class EditVesselViewModelFactory @Inject constructor(
    @IOScheduler private val ioSched: Scheduler,
    private val repo: VesselRepository
) {
    fun getVesselViewModel(vesselID: UUID) : EditVesselViewModel {
        return EditVesselViewModelImpl(
            vesselID,
            ioSched,
            repo
        )
    }
}

class EditVesselViewModelImpl constructor(
    private val vesselID: UUID,
    @IOScheduler private val ioSched: Scheduler,
    private val repo: VesselRepository
): EditVesselViewModel {
    private val eventRelay : PublishRelay<EditVesselOutEvent> = PublishRelay.create()

    override fun updateVessel(newData: Vessel) {
        Observable.just(newData)
            .observeOn(ioSched)
            .map {
                Pair(it,repo.updateVessel(it))
            }.subscribe {
                when(val res = it.second) {
                    is com.lukevanoort.cellarman.util.SuccessIndicator.Success -> {
                        eventRelay.accept(EditVesselOutEvent.VesselUpdated(it.first))
                    }
                    is com.lukevanoort.cellarman.util.SuccessIndicator.Failure -> {
                        eventRelay.accept(EditVesselOutEvent.VesselUpdateFailed(it.first,res.reason))
                    }
                }
            }
    }

    override fun deleteVessel(toDelete: Vessel) {
        Observable.just(toDelete)
            .observeOn(ioSched)
            .map {
                Pair(it,repo.deleteVessel(it))
            }.subscribe {
                when(val res = it.second) {
                    is com.lukevanoort.cellarman.util.SuccessIndicator.Success -> {
                        eventRelay.accept(EditVesselOutEvent.VesselDeleted(it.first))
                    }
                    is com.lukevanoort.cellarman.util.SuccessIndicator.Failure -> {
                        eventRelay.accept(EditVesselOutEvent.VesselDeleteFailed(it.first,res.reason))
                    }
                }
            }
    }

    override fun cancel() {
        eventRelay.accept(EditVesselOutEvent.Cancel)
    }

    override fun getState(): Observable<out EditVesselState> = repo.getVessel(vesselID).map {
            when(it) {
                is MaybeResult.HasResult<Vessel> -> {
                    EditVesselState.HasVessel(it.result)
                }
                else -> {
                    EditVesselState.NoVessel
                }
            }
        }

    override fun getOutput(): Observable<out EditVesselOutEvent> = eventRelay

}