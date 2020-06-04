package com.lukevanoort.cellarman.vessel.logic

import com.jakewharton.rxrelay2.PublishRelay
import com.lukevanoort.cellarman.vessel.model.Vessel
import com.lukevanoort.stuntman.SMViewModel
import io.reactivex.Observable
import java.util.UUID
import javax.inject.Inject

sealed class VesselListViewState {
    data class HasList(val vessels: List<Vessel>) : VesselListViewState()
}

sealed class VesselListOutEvent {
    data class VesselSelected(val id: UUID) : VesselListOutEvent()
}

interface VesselListViewModel :
    SMViewModel<VesselListViewState, VesselListOutEvent> {
    fun vesselSelected(id: UUID)
}

class VesselListViewModelFactory @Inject constructor(
    private val repository: VesselRepository
) {
    fun getVesselListViewModel() : VesselListViewModel {
        return VesselListViewModelImpl(repository)
    }
}

class VesselListViewModelImpl (
    private val repo: VesselRepository
) : VesselListViewModel {
    private val eventBus: PublishRelay<VesselListOutEvent> = PublishRelay.create()

    override fun getState(): Observable<VesselListViewState> = repo.getAllVessels().map { VesselListViewState.HasList(it) }

    override fun getOutput(): Observable<VesselListOutEvent> = eventBus

    override fun vesselSelected(id: UUID) {
        eventBus.accept(VesselListOutEvent.VesselSelected(id))
    }
}