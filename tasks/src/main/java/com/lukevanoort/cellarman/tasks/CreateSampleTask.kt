package com.lukevanoort.cellarman.tasks

import androidx.annotation.UiThread
import com.jakewharton.rxrelay2.BehaviorRelay
import com.lukevanoort.cellarman.sample.logic.*
import com.lukevanoort.sample.model.*
import com.lukevanoort.stuntman.SMCanvas
import com.lukevanoort.stuntman.SMTask
import com.lukevanoort.stuntman.SMTaskReadableDataWad
import com.lukevanoort.stuntman.SMViewModel
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import java.lang.ref.WeakReference
import java.util.Date
import java.util.UUID
import javax.inject.Inject

sealed class CreateSampleTaskOutput {
    data class CreatedSample(val sample: Sample) : CreateSampleTaskOutput()
    object Cancelled : CreateSampleTaskOutput()
}

interface CreateSampleCanvas : SMCanvas {
    fun attachSampleViewModel(vm : CreatingSampleViewModel)
    fun attachSampleTypeViewModel(vm : SampleTypeChooserViewModel)
}

class CreateSampleTaskFactory @Inject constructor(
    val beerSampleVMFactory: CreateBeerSampleViewModelFactory,
    val genericSampleVMFactory: CreateGenericSampleViewModelFactory,
    val attachSampleTypeVMFactory: SampleTypeChooserViewModelFactory
)  {
    fun getViewSampleTask(vesselID: UUID, fillID: UUID?, wad: SMTaskReadableDataWad?) : CreateSampleTask {
        return CreateSampleTask(
            vesselID,
            fillID,
            beerSampleVMFactory,
            genericSampleVMFactory,
            attachSampleTypeVMFactory,
            wad
        )
    }
}

sealed class CreatingSampleVMState {
    data class CreatingBeerSample(val vm: CreateBeerSampleViewModel) : CreatingSampleVMState()
    data class CreatingGenericSample(val vm: CreateGenericSampleViewModel) : CreatingSampleVMState()
}
interface CreatingSampleViewModel :
    SMViewModel<CreatingSampleVMState, Unit>

class CreateSampleTask constructor(
    private val vesselID: UUID,
    private val fillID: UUID?,
    private val beerSampleVMFactory: CreateBeerSampleViewModelFactory,
    private val genericSampleVMFactory: CreateGenericSampleViewModelFactory,
    private val attachSampleTypeVMFactory: SampleTypeChooserViewModelFactory,
    private val wad: SMTaskReadableDataWad?
) : SMTask<CreateSampleCanvas, CreateSampleTaskOutput> {
    private var canvasRef: WeakReference<CreateSampleCanvas?> = WeakReference(null)

    private val eventObs: PublishSubject<CreateSampleTaskOutput> = PublishSubject.create()

    private val tempRelay = BehaviorRelay.createDefault<Sample>(GenericSampleData(
        id = UUID.randomUUID(),
        notes = "",
        fillId = fillID,
        vesselId = vesselID,
        time = Date()
    )
    )

    private val bsVM = beerSampleVMFactory.getSampleViewViewModelFromTemp(
        Consumer<BeerSample> { t -> tempRelay.accept(t) },
        tempRelay.map {
            when(it) {
                is BeerSample -> {
                    it
                }
                else -> {
                    BeerSampleData.fromSample(it)
                }
            }
        }
    )

    private val genVm = genericSampleVMFactory.getSampleViewViewModelFromTemp(
        Consumer<GenericSample> { t -> tempRelay.accept(t) },
        tempRelay.map {
            when(it) {
                is GenericSample -> {
                    it
                }
                else -> {
                    GenericSampleData.fromSample(it)
                }
            }
        }
    )


    val typeRelay = BehaviorRelay.createDefault<SampleType>(SampleType.Beer)
    val sampleTypeVm = attachSampleTypeVMFactory.getSampleViewViewModelFromTemp(typeRelay,typeRelay)


    private val vmSubs = CompositeDisposable()

    init {
        genVm.getOutput().subscribe { o ->
            when(o) {
                is CreateGenericSampleVMOutputs.SampleCreated ->  CreateSampleTaskOutput.CreatedSample(o.sample)
                is CreateGenericSampleVMOutputs.SampleCreationFailed -> null
                CreateGenericSampleVMOutputs.CancelRequested -> CreateSampleTaskOutput.Cancelled
            }?.let { eventObs.onNext(it) }
        }?.also { vmSubs.add(it) }

        bsVM.getOutput().subscribe { o ->
            when(o) {
                is CreateBeerSampleVMOutputs.SampleCreated ->  CreateSampleTaskOutput.CreatedSample(o.sample)
                is CreateBeerSampleVMOutputs.SampleCreationFailed -> null
                CreateBeerSampleVMOutputs.CancelRequested -> CreateSampleTaskOutput.Cancelled
            }?.let { eventObs.onNext(it) }
        }?.also { vmSubs.add(it) }
    }

    @UiThread
    override fun useCanvas(c: CreateSampleCanvas) {
        removeCanvas()
        c.attachSampleViewModel(object : CreatingSampleViewModel {
            override fun getState(): Observable<out CreatingSampleVMState> = typeRelay.map {
                when(it) {
                    SampleType.Beer -> CreatingSampleVMState.CreatingBeerSample(bsVM)
                    SampleType.Generic ->  CreatingSampleVMState.CreatingGenericSample(genVm)
                }
            }

            override fun getOutput(): Observable<out Unit> = Observable.just(Unit)

        })
        c.attachSampleTypeViewModel(sampleTypeVm)
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

    override fun getOutput(): Observable<CreateSampleTaskOutput> = eventObs



}