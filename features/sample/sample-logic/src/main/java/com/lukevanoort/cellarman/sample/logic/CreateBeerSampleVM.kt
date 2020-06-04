package com.lukevanoort.cellarman.sample.logic

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.lukevanoort.cellarman.dagger.IOScheduler
import com.lukevanoort.stuntman.SMViewModel
import com.lukevanoort.cellarman.util.SuccessIndicator
import com.lukevanoort.sample.model.BeerSample
import com.lukevanoort.sample.model.BeerSampleData
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.functions.Consumer
import java.lang.Exception
import java.util.Date
import java.util.UUID
import javax.inject.Inject

sealed class CreateBeerSampleViewState {
    data class CreatingBeerSample(val sample: BeerSample): CreateBeerSampleViewState()
    object NoSample : CreateBeerSampleViewState()
}

sealed class CreateBeerSampleVMOutputs {
    data class SampleCreated(
        val sample: BeerSample
    ) : CreateBeerSampleVMOutputs()

    data class SampleCreationFailed(
        val sample: BeerSample,
        val reason: Exception
    ) : CreateBeerSampleVMOutputs()

    object CancelRequested : CreateBeerSampleVMOutputs()
}

interface CreateBeerSampleViewModel :
    SMViewModel<CreateBeerSampleViewState, CreateBeerSampleVMOutputs> {
    override fun getState() : Observable<CreateBeerSampleViewState>
    override fun getOutput(): Observable<CreateBeerSampleVMOutputs>
    fun updateSample(updatedSample: BeerSample)
    fun createBeerSample(sampleToCreate: BeerSample)
    fun cancel()
}

class CreateBeerSampleViewModelFactory @Inject constructor(
    private val repository: SampleRepository,
    @IOScheduler private val ioSched: Scheduler
) {

    fun getSampleViewViewModelFromTemp(
        tempStateConsumer: Consumer<BeerSample>,
        tempStateSource: Observable<BeerSample>
    ) : CreateBeerSampleViewModel {
        return CreateBeerSampleViewModelImpl(
            repository,
            ioSched,
            tempStateConsumer,
            tempStateSource
        )
    }

    fun getSampleViewViewModel(vesselId: UUID) : CreateBeerSampleViewModel {
        val relay = BehaviorRelay.createDefault(
            BeerSampleData(
                id = UUID.randomUUID(),
                fillId = null,
                vesselId = vesselId,
                time = Date(),
                notes = "",
                aceticLevel = null,
                funkLevel = null,
                sourLevel = null,
                ropeLevel = null,
                oakLevel = null
            ) as BeerSample
        )
        return getSampleViewViewModelFromTemp(
            relay,
            relay
        )
    }
}

class CreateBeerSampleViewModelImpl (
    private val repo: SampleRepository,
    private val ioSched: Scheduler,
    private val tempStateConsumer: Consumer<BeerSample>,
    private val tempStateSource: Observable<BeerSample>
) : CreateBeerSampleViewModel {
    private val eventRelay: PublishRelay<CreateBeerSampleVMOutputs> = PublishRelay.create()

    override fun getState(): Observable<CreateBeerSampleViewState> = tempStateSource.map {
        CreateBeerSampleViewState.CreatingBeerSample(it)
    }

    override fun getOutput(): Observable<CreateBeerSampleVMOutputs> = eventRelay

    override fun updateSample(updatedSample: BeerSample) {
        tempStateConsumer.accept(updatedSample)
    }

    override fun createBeerSample(sampleToCreate: BeerSample) {
        Observable.just(sampleToCreate)
            .observeOn(ioSched)
            .map {
                Pair(it,repo.recordSample(it))
            }
            .subscribe {
                when(val res = it.second) {
                    com.lukevanoort.cellarman.util.SuccessIndicator.Success -> {
                        eventRelay.accept(CreateBeerSampleVMOutputs.SampleCreated(it.first))
                    }
                    is com.lukevanoort.cellarman.util.SuccessIndicator.Failure -> {
                        eventRelay.accept(CreateBeerSampleVMOutputs.SampleCreationFailed(it.first,res.reason))
                    }

                }
            }
    }

    override fun cancel() {
        eventRelay.accept(CreateBeerSampleVMOutputs.CancelRequested)
    }

}