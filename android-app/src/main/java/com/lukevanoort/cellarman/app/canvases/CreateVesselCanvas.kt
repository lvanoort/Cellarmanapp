package com.lukevanoort.cellarman.app.canvases

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.lukevanoort.cellarman.app.R
import com.lukevanoort.stuntman.SMBackPressedListener
import com.lukevanoort.cellarman.tasks.CreateVesselCanvas
import com.lukevanoort.cellarman.vessel.logic.CreateVesselViewModel
import com.lukevanoort.cellarman.vessel.logic.CreateVesselViewState
import com.lukevanoort.cellarman.vessel.model.VesselData
import io.reactivex.disposables.Disposable
import javax.inject.Inject

private const val LAYOUT_ID = R.layout.create_vessel_canvas

class CreateVesselCanvasFactory @Inject constructor() {
    fun createCanvasInActivity(act:Activity) : CreateVesselCanvas {
        act.setContentView(LAYOUT_ID)
        return CreateVesselCanvasImpl(
            act.findViewById<View>(
                R.id.create_vessel_canvas
            )
        )
    }
}

class CreateVesselCanvasImpl constructor(val vesselView: View) : CreateVesselCanvas, SMBackPressedListener {
    private var currentVm: CreateVesselViewModel? = null
    private var disposable: Disposable? = null
    private val vmSyncLock: Any = Any()

    private val nameTextView: EditText = vesselView.findViewById(R.id.et_vessel_name)
    private val backButton: Button = vesselView.findViewById(R.id.bt_back)
    private val okButton: Button = vesselView.findViewById(R.id.bt_ok)

    private var nextState : CreateVesselViewState? = null
    private var currentState : CreateVesselViewState? = null

    init {
        okButton.setOnClickListener {
            currentState?.let {
                when(it) {
                    is CreateVesselViewState.CreatingVessel -> currentVm?.createVessel(it.vessel)
                }
            }
        }

        backButton.setOnClickListener {
            currentVm?.cancel()
        }

        nameTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                currentState?.let {
                    if (it is CreateVesselViewState.CreatingVessel) {
                        val newVessel = VesselData.from(it.vessel).copy(
                            name = s.toString()
                        )
                        currentVm?.updateVessel(newVessel)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })
    }

    private fun reset() {
        val localCurrent = currentState
        val localNext = nextState
        if (localCurrent != localNext) {
            if (localNext != null) {
                when(localNext) {
                    is CreateVesselViewState.CreatingVessel -> {
                        if (nameTextView.text.toString() != localNext.vessel.name) {
                            nameTextView.setText(localNext.vessel.name)
                        }
                    }
                }
            } else {
                nameTextView.setText("");
            }
            currentState = localNext
        }

    }

    override fun attachCreateVesselViewModel(vm: CreateVesselViewModel) {
        synchronized(vmSyncLock) {
            disposable?.dispose()
            currentVm = vm
            disposable = vm.getState().subscribe {
                nextState = it
                vesselView.post {
                    reset()
                }
            }
        }
    }

    override fun detachAllViewModels() {
        synchronized(vmSyncLock) {
            disposable?.dispose()
            currentVm = null
        }
    }

    override fun backPressed(): Boolean {
        currentVm?.cancel()
        return true
    }

}