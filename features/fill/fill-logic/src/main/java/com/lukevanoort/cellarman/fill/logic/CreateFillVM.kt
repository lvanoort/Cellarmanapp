package com.lukevanoort.cellarman.fill.logic

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.lukevanoort.cellarman.dagger.IOScheduler
import com.lukevanoort.cellarman.fill.model.Fill
import com.lukevanoort.cellarman.fill.model.FillData
import com.lukevanoort.cellarman.fill.model.FillType
import com.lukevanoort.stuntman.SMViewModel
import com.lukevanoort.cellarman.util.SuccessIndicator
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.functions.Consumer
import java.lang.Exception
import java.util.Date
import java.util.UUID
import javax.inject.Inject

sealed class CreateFillViewState {
    data class CreatingFill(val fill: Fill): CreateFillViewState()
    object NoFill : CreateFillViewState()
}

sealed class CreateFillVMOutputs {
    data class FillCreated(
        val fill: Fill
    ) : CreateFillVMOutputs()

    data class FillCreationFailed(
        val fill: Fill,
        val reason: Exception
    ) : CreateFillVMOutputs()
    object CancelRequested : CreateFillVMOutputs()
}

interface CreateFillViewModel :
    SMViewModel<CreateFillViewState, CreateFillVMOutputs> {
    override fun getState() : Observable<CreateFillViewState>
    override fun getOutput(): Observable<CreateFillVMOutputs>
    fun updateFill(updatedFill: Fill)
    fun createFill(fillToCreate: Fill)
    fun cancel()
}



class CreateFillViewModelFactory @Inject constructor(
    private val repository: FillRepository,
    @IOScheduler private val ioSched: Scheduler
) {

    fun getFillViewViewModelFromTemp(
        tempStateConsumer: Consumer<Fill>,
        tempStateSource: Observable<Fill>) : CreateFillViewModel {
        return CreateFillViewModelImpl(
            repository,
            ioSched,
            tempStateConsumer,
            tempStateSource
        )
    }

    fun getFillViewViewModel(vesselId: UUID) : CreateFillViewModel {
        val relay = BehaviorRelay.createDefault(
            FillData(
                id = UUID.randomUUID(),
                vesselId = vesselId,
                type = FillType.Beer,
                time = Date(),
                notes = ""
            ) as Fill
        )
        return getFillViewViewModelFromTemp(
            relay,
            relay
        )
    }
}

class CreateFillViewModelImpl (
    private val repo: FillRepository,
    private val ioSched: Scheduler,
    private val tempStateConsumer: Consumer<Fill>,
    private val tempStateSource: Observable<Fill>
) : CreateFillViewModel {
    private val eventRelay: PublishRelay<CreateFillVMOutputs> = PublishRelay.create()

    override fun getState(): Observable<CreateFillViewState> = tempStateSource.map {
        CreateFillViewState.CreatingFill(it)
    }

    override fun getOutput(): Observable<CreateFillVMOutputs> = eventRelay

    override fun updateFill(updatedFill: Fill) {
        tempStateConsumer.accept(updatedFill)
    }

    override fun createFill(fillToCreate: Fill) {
        Observable.just(fillToCreate)
            .observeOn(ioSched)
            .map {
                Pair(it,repo.recordFill(it))
            }
            .subscribe {
                when(val res = it.second) {
                    com.lukevanoort.cellarman.util.SuccessIndicator.Success -> {
                        eventRelay.accept(CreateFillVMOutputs.FillCreated(it.first))
                    }
                    is com.lukevanoort.cellarman.util.SuccessIndicator.Failure -> {
                        eventRelay.accept(CreateFillVMOutputs.FillCreationFailed(it.first,res.reason))
                    }

                }
            }
    }

    override fun cancel() {
        eventRelay.accept(CreateFillVMOutputs.CancelRequested)
    }

}