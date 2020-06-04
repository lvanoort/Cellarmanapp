package com.lukevanoort.cellarman.sample.logic

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.lukevanoort.cellarman.dagger.IOScheduler
import com.lukevanoort.cellarman.fill.model.Fill
import com.lukevanoort.stuntman.SMViewModel
import com.lukevanoort.cellarman.util.SuccessIndicator
import com.lukevanoort.sample.model.GenericSample
import com.lukevanoort.sample.model.GenericSampleData
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.functions.Consumer
import java.lang.Exception
import java.util.Date
import java.util.UUID
import javax.inject.Inject

sealed class CreateGenericSampleViewState {
    data class CreatingGenericSample(
        val sample: GenericSample,
        val fillOptions: List<Fill>
    ): CreateGenericSampleViewState()
    object NoSample : CreateGenericSampleViewState()
}

sealed class CreateGenericSampleVMOutputs {
    data class SampleCreated(
        val sample: GenericSample
    ) : CreateGenericSampleVMOutputs()

    data class SampleCreationFailed(
        val sample: GenericSample,
        val reason: Exception
    ) : CreateGenericSampleVMOutputs()

    object CancelRequested : CreateGenericSampleVMOutputs()
}

interface CreateGenericSampleViewModel :
    SMViewModel<CreateGenericSampleViewState, CreateGenericSampleVMOutputs> {
    override fun getState() : Observable<CreateGenericSampleViewState>
    override fun getOutput(): Observable<CreateGenericSampleVMOutputs>
    fun updateSample(updatedSample: GenericSample)
    fun createGenericSample(sampleToCreate: GenericSample)
    fun cancel()
}

class CreateGenericSampleViewModelFactory @Inject constructor(
    private val repository: SampleRepository,
    @IOScheduler private val ioSched: Scheduler
) {

    fun getSampleViewViewModelFromTemp(
        tempStateConsumer: Consumer<GenericSample>,
        tempStateSource: Observable<GenericSample>
    ) : CreateGenericSampleViewModel {
        return CreateGenericSampleViewModelImpl(
            repository,
            ioSched,
            tempStateConsumer,
            tempStateSource
        )
    }

    fun getSampleViewViewModel(vesselId: UUID) : CreateGenericSampleViewModel {
        val relay = BehaviorRelay.createDefault(
            GenericSampleData(
                id = UUID.randomUUID(),
                fillId = null,
                vesselId = vesselId,
                time = Date(),
                notes = ""
            ) as GenericSample
        )
        return getSampleViewViewModelFromTemp(
            relay,
            relay
        )
    }
}

class CreateGenericSampleViewModelImpl (
    private val repo: SampleRepository,
    private val ioSched: Scheduler,
    private val tempStateConsumer: Consumer<GenericSample>,
    private val tempStateSource: Observable<GenericSample>
) : CreateGenericSampleViewModel {
    private val eventRelay: PublishRelay<CreateGenericSampleVMOutputs> = PublishRelay.create()

    override fun getState(): Observable<CreateGenericSampleViewState> = tempStateSource.switchMap {
        // this is theoretically sound and quick to write, but inefficient
        repo.getVesselFillHistory(vesselID = it.id).map {fills ->
            CreateGenericSampleViewState.CreatingGenericSample(it, fills)
        }
    }

    override fun getOutput(): Observable<CreateGenericSampleVMOutputs> = eventRelay

    override fun updateSample(updatedSample: GenericSample) {
        tempStateConsumer.accept(updatedSample)
    }

    override fun createGenericSample(sampleToCreate: GenericSample) {
        Observable.just(sampleToCreate)
            .observeOn(ioSched)
            .map {
                Pair(it,repo.recordSample(it))
            }
            .subscribe {
                when(val res = it.second) {
                    com.lukevanoort.cellarman.util.SuccessIndicator.Success -> {
                        eventRelay.accept(CreateGenericSampleVMOutputs.SampleCreated(it.first))
                    }
                    is com.lukevanoort.cellarman.util.SuccessIndicator.Failure -> {
                        eventRelay.accept(CreateGenericSampleVMOutputs.SampleCreationFailed(it.first,res.reason))
                    }

                }
            }
    }

    override fun cancel() {
        eventRelay.accept(CreateGenericSampleVMOutputs.CancelRequested)
    }

}