package com.lukevanoort.cellarman.tasks

import androidx.annotation.UiThread
import com.jakewharton.rxrelay2.BehaviorRelay
import com.lukevanoort.cellarman.dagger.AppScope
import com.lukevanoort.cellarman.dagger.RootTaskWad
import com.lukevanoort.cellarman.util.HistoricalStackManager
import com.lukevanoort.cellarman.util.TransientStackManager
import com.lukevanoort.cellarman.util.asDefault
import com.lukevanoort.stuntman.*
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.lang.IllegalArgumentException
import java.lang.ref.WeakReference
import java.util.UUID
import javax.inject.Inject

sealed class RootTaskAddress {
    object HomeScreen : RootTaskAddress()
    object CreatingVessel : RootTaskAddress()
    data class ManageVessel(val vesselID: UUID) : RootTaskAddress()
    data class CreateSample(val vesselID: UUID) : RootTaskAddress()
    data class CreateFill(val vesselID: UUID) : RootTaskAddress()

    companion object {
        fun toParsableString(address : RootTaskAddress) : String = when(address){
            HomeScreen -> "0:home"
            CreatingVessel -> "0:creating"
            is ManageVessel -> "0:manage-vessel:"+address.vesselID.toString()
            is CreateSample -> "0:create-sample:"+address.vesselID.toString()
            is CreateFill -> "0:create-fill:"+address.vesselID.toString()
        }

        fun parseFromString(string : String) : RootTaskAddress? = when(string){
            "0:home" -> HomeScreen
            "0:creating" -> CreatingVessel
            else -> {
                if (string.startsWith("0:manage-vessel:")) {
                    val split = string.split(":")
                    if (split.size >= 3) {
                        try {
                            val vesselID = UUID.fromString(split[2])
                            ManageVessel(vesselID)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    } else {
                        null
                    }
                } else if (string.startsWith("0:create-sample:")) {
                    val split = string.split(":")
                    if (split.size >= 3) {
                        try {
                            val vesselID = UUID.fromString(split[2])
                            ManageVessel(vesselID)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    } else {
                        null
                    }
                }  else if (string.startsWith("0:create-fill:")) {
                    val split = string.split(":")
                    if (split.size >= 3) {
                        try {
                            val vesselID = UUID.fromString(split[2])
                            ManageVessel(vesselID)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }
    }
}

sealed class RootTaskState {
    data class CreatingVessel(val createVesselTask: CreateVesselTask) : RootTaskState()
    data class HomeScreen(val homeScreenTask: HomeScreenTask) : RootTaskState()
    data class ViewingVessel(val viewingVesselTask: ManageVesselTask) : RootTaskState()
    data class CreatingSample(val createSampleTask: CreateSampleTask) : RootTaskState()
    data class CreatingFill(val createFillTask: CreateFillTask) : RootTaskState()
}

interface RootCanvas : SMCanvas {
    fun attachRootTaskViewModel(vm : RootTaskViewModel)
}

interface RootTaskViewModel :
    SMViewModel<RootTaskState, Unit>

@AppScope
class RootTask @Inject constructor(
    @RootTaskWad val rootWad: SMTaskReadableDataWad,
    val hsTaskProvider: HomeScreenTaskFactory,
    val creatingVesselTask: CreateVesselTaskFactory,
    val viewingVesselTaskFactory: ManageVesselTaskFactory,
    val createSampleTaskFactory: CreateSampleTaskFactory,
    val createFillTaskFactory: CreateFillTaskFactory
): SMTask<RootCanvas, Unit> {
    private var stateRelay : BehaviorRelay<RootTaskState> = BehaviorRelay.create();
    private var canvasRef: WeakReference<RootCanvas?> = WeakReference(null)

    private val rootVm = object : RootTaskViewModel {
        override fun getState(): Observable<RootTaskState> = stateRelay
        override fun getOutput(): Observable<Unit> = Observable.just(Unit)
    }

    private var task: SMTask<*, *>? = null
    private var taskDisposable: Disposable? = null

    private val stateLock = Any()
    private var currentState: RootTaskAddress? = null

    private val stackManager: HistoricalStackManager<RootTaskAddress>
    private val stackSub: Disposable

    init {
        stackManager = rootWad.getStringArray(NAV_STACK_WAD_KEY)?.mapNotNull {
            RootTaskAddress.parseFromString(it)
        }?.let {
            if (it.isNotEmpty()) {
                goToAddress(it.last(),true)
                TransientStackManager<RootTaskAddress>(it)
            } else {
                TransientStackManager<RootTaskAddress>()
            }
        } ?: TransientStackManager<RootTaskAddress>()

        stackSub = stackManager.getLatest().subscribe {
            goToAddress(it.asDefault(RootTaskAddress.HomeScreen).result,false)
        }
    }

    private fun goToAddress(newState : RootTaskAddress, useWad: Boolean) {
        synchronized(stateLock) {
            val localCurrent = currentState
            if (localCurrent != newState) {
                taskDisposable?.dispose()
                task?.finished()
                when(newState) {
                    RootTaskAddress.HomeScreen -> {
                        val newTask = hsTaskProvider.getViewVesselTask(if(useWad) {
                            rootWad.provideChildReadableNamespacedWad(CHILD_HOME_WAD_KEY)
                        } else {
                            null
                        })
                        task = newTask
                        taskDisposable = newTask.getOutput().subscribe {
                            when(it) {
                                is HomeScreenOutput.RequestedViewVessel -> {
                                    stackManager.push(RootTaskAddress.ManageVessel(it.id))
                                }
                                HomeScreenOutput.RequestedCreateVessel -> {
                                    stackManager.push(RootTaskAddress.CreatingVessel)
                                }
                            }
                        }
                        stateRelay.accept(RootTaskState.HomeScreen(newTask))
                    }
                    RootTaskAddress.CreatingVessel -> {
                        val newTask = creatingVesselTask.getViewVesselTask(if(useWad) {
                            rootWad.provideChildReadableNamespacedWad(CHILD_CREATING_VESSEL_KEY)
                        } else {
                            null
                        })
                        task = newTask
                        taskDisposable = newTask.getOutput().subscribe {
                            when(it) {
                                is CreateVesselOutput.CreatedVessel,
                                CreateVesselOutput.Cancelled -> {
                                    stackManager.pop()
                                }
                            }
                        }
                        stateRelay.accept(RootTaskState.CreatingVessel(newTask))
                    }
                    is RootTaskAddress.ManageVessel -> {
                        val newTask = viewingVesselTaskFactory.getViewVesselTask(newState.vesselID,if(useWad) {
                            rootWad.provideChildReadableNamespacedWad(CHILD_VIEWING_VESSEL_KEY)
                        } else {
                            null
                        })
                        task = newTask
                        taskDisposable = newTask.getOutput().subscribe {
                            when(it) {
                                is ManageVesselOutput.AddSample -> {
                                    stackManager.push(RootTaskAddress.CreateSample(it.vesselID))
                                }
                                is ManageVesselOutput.AddFill -> {
                                    stackManager.push(RootTaskAddress.CreateFill(it.vesselID))
                                }
                                is ManageVesselOutput.Cancelled -> {
                                    stackManager.pop()
                                }
                            }
                        }
                        stateRelay.accept(RootTaskState.ViewingVessel(newTask))
                    }
                    is RootTaskAddress.CreateSample -> {
                        val newTask = createSampleTaskFactory.getViewSampleTask(newState.vesselID,null,if(useWad) {
                            rootWad.provideChildReadableNamespacedWad(CHILD_CREATING_SAMPLE_VESSEL_KEY)
                        } else {
                            null
                        })
                        task = newTask
                        taskDisposable = newTask.getOutput().subscribe {
                            when(it) {
                                is CreateSampleTaskOutput.CreatedSample,
                                CreateSampleTaskOutput.Cancelled -> {
                                    stackManager.pop()
                                }
                            }
                        }
                        stateRelay.accept(RootTaskState.CreatingSample(newTask))
                    }
                    is RootTaskAddress.CreateFill -> {
                        val newTask = createFillTaskFactory.getCreateFillTask(newState.vesselID,if(useWad) {
                            rootWad.provideChildReadableNamespacedWad(CHILD_CREATING_FILL_VESSEL_KEY)
                        } else {
                            null
                        })
                        task = newTask
                        taskDisposable = newTask.getOutput().subscribe {
                            when(it) {
                                is CreateFillOutput.CreatedFill,
                                CreateFillOutput.Cancelled -> {
                                    stackManager.pop()
                                }
                            }
                        }
                        stateRelay.accept(RootTaskState.CreatingFill(newTask))
                    }

                }.let{}

            }
        }
    }

    @UiThread
    override fun useCanvas(c: RootCanvas) {
        removeCanvas()
        c.attachRootTaskViewModel(rootVm)
        canvasRef = WeakReference(c)
    }

    @UiThread
    override fun removeCanvas() {
        canvasRef.get()?.detachAllViewModels()
        canvasRef = WeakReference(null)
    }


    override fun getOutput(): Observable<Unit> = Observable.just(Unit)

    override fun writeWad(wad: SMTaskWritableDataWad) {
        super.writeWad(wad)

        synchronized(stateLock) {
            val localCurrent = currentState

            wad.store(NAV_STACK_WAD_KEY, value = stackManager.getStack().map {
                RootTaskAddress.toParsableString(it)
            }.toTypedArray())
            when(localCurrent) {
                RootTaskAddress.HomeScreen -> wad.provideChildWritableNamespacedWad(CHILD_HOME_WAD_KEY)
                RootTaskAddress.CreatingVessel -> wad.provideChildWritableNamespacedWad(CHILD_CREATING_VESSEL_KEY)
                is RootTaskAddress.ManageVessel -> wad.provideChildWritableNamespacedWad(CHILD_VIEWING_VESSEL_KEY)
                is RootTaskAddress.CreateSample -> wad.provideChildWritableNamespacedWad(CHILD_CREATING_SAMPLE_VESSEL_KEY)
                is RootTaskAddress.CreateFill -> wad.provideChildWritableNamespacedWad(CHILD_CREATING_FILL_VESSEL_KEY)
                null -> null
            }?.let { task?.writeWad(it) }
        }
    }

    override fun finished() {
        // finishing the root task doesnt make sense
    }

    companion object {
        private const val NAV_STACK_WAD_KEY = "NAV_STACK"
        private const val CHILD_HOME_WAD_KEY = "CHILD_HOME"
        private const val CHILD_CREATING_VESSEL_KEY = "CHILD_CREATING"
        private const val CHILD_VIEWING_VESSEL_KEY = "CHILD_VIEWING"
        private const val CHILD_CREATING_SAMPLE_VESSEL_KEY = "CHILD_CREATING_SAMPLE"
        private const val CHILD_CREATING_FILL_VESSEL_KEY = "CHILD_CREATING_FILL"
    }
}