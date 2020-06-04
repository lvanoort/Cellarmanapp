package com.lukevanoort.cellarman.fill.logic

import com.jakewharton.rxrelay2.PublishRelay
import com.lukevanoort.cellarman.fill.model.Fill
import com.lukevanoort.stuntman.SMViewModel
import io.reactivex.Observable
import java.util.UUID
import javax.inject.Inject

sealed class FillListViewState {
    data class HasList(val fills: List<Fill>) : FillListViewState()
}

sealed class FillListOutEvent {
    data class FillSelected(val id: UUID) : FillListOutEvent()
}

interface FillListViewModel :
    SMViewModel<FillListViewState, FillListOutEvent> {
    fun fillSelected(id: UUID)
}

class FillListViewModelFactory @Inject constructor(
    private val repository: FillRepository
) {
    fun getVesselFillListViewModel(vesselID: UUID) : FillListViewModel {
        return FillListViewModelImpl(vesselID,repository)
    }


    fun getAllFillListViewModel() : FillListViewModel {
        return FillListViewModelImpl(null,repository)
    }
}

class FillListViewModelImpl (
    private val vesselID: UUID?,
    private val repo: FillRepository
) : FillListViewModel {
    private val eventBus: PublishRelay<FillListOutEvent> = PublishRelay.create()

    override fun getState(): Observable<FillListViewState> = if (vesselID != null) {
        repo.getVesselFillHistory(vesselID)
    } else {
        repo.getFillHistory()
    }.map { FillListViewState.HasList(it) }

    override fun getOutput(): Observable<FillListOutEvent> = eventBus

    override fun fillSelected(id: UUID) {
        eventBus.accept(FillListOutEvent.FillSelected(id))
    }
}