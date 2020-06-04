package com.lukevanoort.cellarman.sample.logic

import com.jakewharton.rxrelay2.PublishRelay
import com.lukevanoort.sample.model.Sample
import com.lukevanoort.stuntman.SMViewModel
import io.reactivex.Observable
import java.util.UUID
import javax.inject.Inject

sealed class SampleListViewState {
    data class HasList(val samples: List<Sample>) : SampleListViewState()
}

sealed class SampleListOutEvent {
    data class SampleSelected(val id: UUID) : SampleListOutEvent()
}

interface SampleListViewModel :
    SMViewModel<SampleListViewState, SampleListOutEvent> {
    fun sampleSelected(id: UUID)
}

class SampleListViewModelFactory @Inject constructor(
    private val repository: SampleRepository
) {
    fun getVesselSampleListViewModel(vesselID: UUID) : SampleListViewModel {
        return SampleListViewModelImpl(
            vesselID = vesselID,
            repo = repository
        )
    }

    fun getFillSampleListViewModel(fillID: UUID) : SampleListViewModel {
        return SampleListViewModelImpl(
            fillID = fillID,
            repo = repository
        )
    }


    fun getAllSampleListViewModel() : SampleListViewModel {
        return SampleListViewModelImpl(
            repo = repository
        )
    }
}

class SampleListViewModelImpl (
    private val vesselID: UUID? = null,
    private val fillID: UUID? = null,
    private val repo: SampleRepository
) : SampleListViewModel {
    private val eventBus: PublishRelay<SampleListOutEvent> = PublishRelay.create()

    override fun getState(): Observable<SampleListViewState> = if (vesselID != null) {
        repo.getVesselSampleHistory(vesselID)
    } else {
        repo.getSampleHistory()
    }.map { SampleListViewState.HasList(it) }

    override fun getOutput(): Observable<SampleListOutEvent> = eventBus

    override fun sampleSelected(id: UUID) {
        eventBus.accept(SampleListOutEvent.SampleSelected(id))
    }
}