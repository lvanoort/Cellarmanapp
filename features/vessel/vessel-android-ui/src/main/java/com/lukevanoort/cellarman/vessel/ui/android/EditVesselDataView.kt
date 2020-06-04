package com.lukevanoort.cellarman.vessel.ui.android

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.constraintlayout.widget.ConstraintLayout
import com.lukevanoort.cellarman.vessel.logic.EditVesselState
import com.lukevanoort.cellarman.vessel.logic.EditVesselViewModel
import com.lukevanoort.stuntman.SMView
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.vessel_data_view.view.*


class EditVesselDataView : ConstraintLayout,
    SMView<EditVesselState, EditVesselViewModel> {
    private var currentState : EditVesselState = EditVesselState.NoVessel
    private var nextState : EditVesselState = EditVesselState.NoVessel

    private var vm : EditVesselViewModel? = null
    private var disp : Disposable? = null

    constructor(context: Context) : super(context) { initialize(context) }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { initialize(context) }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) { initialize(context) }

    private fun initialize(ctx: Context) {
        LayoutInflater.from(ctx).inflate(R.layout.vessel_data_view,this)
    }

    override fun bindViewModel(vm : EditVesselViewModel) {
        disp?.dispose()
        this.vm = vm
        disp = vm.getState().subscribe {
            setState(it)
        }
    }

    override fun unbindViewModel() {
        disp?.dispose()
        this.vm = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unbindViewModel()
    }

    @AnyThread
    override fun setState (state : EditVesselState) {
        this.nextState = state
        post { reset() }
    }

    @UiThread
    private fun reset() {
        val localNext = nextState
        val localCurrent = currentState
        if (localNext != localCurrent) {
            when(localNext){
                is EditVesselState.HasVessel -> {
                    tv_vessel_name.setText(localNext.vessel.name)
                    tv_vessel_description.text = localNext.vessel.getConstructionShortSummary(context)
                    tv_vessel_notes.setText(localNext.vessel.notes)

                }
                EditVesselState.NoVessel -> {
                    tv_vessel_name.setText("")
                    tv_vessel_description.setText( "")
                    tv_vessel_notes.setText( "")
                }
            }.let { currentState = localNext }
        }
    }
}