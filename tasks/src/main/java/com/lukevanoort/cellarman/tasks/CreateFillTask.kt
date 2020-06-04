package com.lukevanoort.cellarman.tasks

import androidx.annotation.UiThread
import com.jakewharton.rxrelay2.BehaviorRelay
import com.lukevanoort.stuntman.SMTask
import com.lukevanoort.cellarman.fill.*
import com.lukevanoort.cellarman.fill.logic.CreateFillVMOutputs
import com.lukevanoort.cellarman.fill.logic.CreateFillViewModel
import com.lukevanoort.cellarman.fill.logic.CreateFillViewModelFactory
import com.lukevanoort.cellarman.fill.model.Fill
import com.lukevanoort.cellarman.fill.model.FillData
import com.lukevanoort.cellarman.fill.model.FillType
import com.lukevanoort.stuntman.SMCanvas
import com.lukevanoort.stuntman.SMTaskReadableDataWad
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.lang.ref.WeakReference
import java.util.Date
import java.util.UUID
import javax.inject.Inject

sealed class CreateFillOutput {
    data class CreatedFill(val fill: Fill) : CreateFillOutput()
    object Cancelled : CreateFillOutput()
}

interface CreateFillCanvas : SMCanvas {
    fun attachCreateFillViewModel(vm : CreateFillViewModel)
}

class CreateFillTaskFactory @Inject constructor(
    val createFillVMFactory: CreateFillViewModelFactory
)  {
    fun getCreateFillTask(
        vesselID : UUID,
        wad: SMTaskReadableDataWad?
    ) : CreateFillTask {
        return CreateFillTask(
            vesselID,
            createFillVMFactory
        )
    }
}

class CreateFillTask constructor(
    private val vesselID: UUID,
    createFillVMFactory: CreateFillViewModelFactory
) : SMTask<CreateFillCanvas, CreateFillOutput> {
    private var canvasRef: WeakReference<CreateFillCanvas?> = WeakReference(null)

    private val eventObs: PublishSubject<CreateFillOutput> = PublishSubject.create()

    private val tempRelay = BehaviorRelay.create<Fill>()

    private val cvVm = createFillVMFactory.getFillViewViewModelFromTemp(tempRelay,tempRelay)

    private val vmSubs = CompositeDisposable()

    init {
        cvVm.getOutput().subscribe { o ->
            when(o) {
                is CreateFillVMOutputs.FillCreated -> {
                    eventObs.onNext(CreateFillOutput.CreatedFill(o.fill))
                    resetFill()
                }
                is CreateFillVMOutputs.FillCreationFailed -> {
                    //ignore
                }
                CreateFillVMOutputs.CancelRequested -> {
                    eventObs.onNext(CreateFillOutput.Cancelled)
                    resetFill()
                }
            }
        }?.also { vmSubs.add(it) }
    }

    init {
        resetFill()
    }

    private fun resetFill() {
        tempRelay.accept(FillData(
            id = UUID.randomUUID(),
            vesselId = vesselID,
            notes = "",
            time = Date(),
            type = FillType.Beer
        ) as Fill)
    }

    @UiThread
    override fun useCanvas(c: CreateFillCanvas) {
        removeCanvas()
        c.attachCreateFillViewModel(cvVm)
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

    override fun getOutput(): Observable<CreateFillOutput> = eventObs



}