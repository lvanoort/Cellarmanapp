package com.lukevanoort.cellarman.sample.logic

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.lukevanoort.sample.model.SampleType
import com.lukevanoort.stuntman.SMViewModel
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import javax.inject.Inject

sealed class SampleTypeChooserViewState {
    data class TypeChosen(val sampleType: SampleType): SampleTypeChooserViewState()
}

sealed class SampleTypeChooserVMOutputs {
    data class SwitchedTo(
        val sampleType: SampleType
    ) : SampleTypeChooserVMOutputs()
}

interface SampleTypeChooserViewModel :
    SMViewModel<SampleTypeChooserViewState, SampleTypeChooserVMOutputs> {
    override fun getState() : Observable<SampleTypeChooserViewState>
    override fun getOutput(): Observable<SampleTypeChooserVMOutputs>
    fun switchTo(sampleType: SampleType)
}

class SampleTypeChooserViewModelFactory @Inject constructor(
) {

    fun getSampleViewViewModelFromTemp(
        tempStateConsumer: Consumer<SampleType>,
        tempStateSource: Observable<SampleType>
    ) : SampleTypeChooserViewModel {
        return SampleTypeChooserViewModelImpl(
            tempStateConsumer,
            tempStateSource
        )
    }

    fun getSampleViewViewModel() : SampleTypeChooserViewModel {
        val relay = BehaviorRelay.createDefault(
            SampleType.Generic as SampleType
        )
        return getSampleViewViewModelFromTemp(
            relay,
            relay
        )
    }
}

class SampleTypeChooserViewModelImpl (
    private val tempStateConsumer: Consumer<SampleType>,
    private val tempStateSource: Observable<SampleType>
) : SampleTypeChooserViewModel {
    private val eventRelay: PublishRelay<SampleTypeChooserVMOutputs> = PublishRelay.create()

    override fun getState(): Observable<SampleTypeChooserViewState> = tempStateSource.map {
        SampleTypeChooserViewState.TypeChosen(it)
    }

    override fun getOutput(): Observable<SampleTypeChooserVMOutputs> = eventRelay

    override fun switchTo(sampleType: SampleType) {
        tempStateConsumer.accept(sampleType)
        eventRelay.accept(SampleTypeChooserVMOutputs.SwitchedTo(sampleType))
    }

}