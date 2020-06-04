package com.lukevanoort.cellarman.fill.ui.android

import android.content.Context
import android.database.DataSetObserver
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import androidx.annotation.AnyThread
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import com.lukevanoort.cellarman.fill.logic.CreateFillViewModel
import com.lukevanoort.cellarman.fill.logic.CreateFillViewState
import com.lukevanoort.cellarman.fill.model.FillData
import com.lukevanoort.cellarman.fill.model.FillType
import com.lukevanoort.stuntman.SMView
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fill_create_fill_layout.view.*

class CreateFillView : ConstraintLayout,
    SMView<CreateFillViewState, CreateFillViewModel> {
    private var currentState : CreateFillViewState = CreateFillViewState.NoFill
    private var nextState : CreateFillViewState = CreateFillViewState.NoFill

    private var vm : CreateFillViewModel? = null
    private var disp : Disposable? = null

    private inner class FillTypeOption(val type: FillType, @StringRes val descriptionRes: Int) {
        override fun toString(): String = context.getString(descriptionRes) 
    }
    
    private val options = listOf(
        FillTypeOption(FillType.Wine,R.string.filltype_wine),
        FillTypeOption(FillType.Beer,R.string.filltype_beer),
        FillTypeOption(FillType.Gin,R.string.filltype_gin),
        FillTypeOption(FillType.Whisky,R.string.filltype_whisky),
        FillTypeOption(FillType.Water,R.string.filltype_water),
        FillTypeOption(FillType.HoldingSolution,R.string.filltype_holding_solution),
        FillTypeOption(FillType.SulphurDioxide,R.string.filltype_sulphur_dioxide),
        FillTypeOption(FillType.Air,R.string.filltype_air),
        FillTypeOption(FillType.PBW,R.string.filltype_pbw),
        FillTypeOption(FillType.StarSan,R.string.filltype_starsan),
        FillTypeOption(FillType.Other,R.string.filltype_other)
    )

    constructor(context: Context) : super(context) { initialize(context) }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { initialize(context) }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) { initialize(context) }

    private fun initialize(ctx: Context) {
        LayoutInflater.from(ctx).inflate(R.layout.fill_create_fill_layout,this)
        et_notes.addTextChangedListener { et ->
            val localState = currentState
            if (localState is CreateFillViewState.CreatingFill) {
                FillData.fromFill(localState.fill).copy(
                    notes = et.toString()
                ).let {bsd ->
                    vm?.updateFill(bsd)
                }
            }
        }
        acb_save.setOnClickListener {
            val localState = currentState
            if (localState is CreateFillViewState.CreatingFill) {
                vm?.createFill(localState.fill)
            }
        }

        acb_cancel.setOnClickListener {
            vm?.cancel()
        }

        ArrayAdapter(
            ctx,
            android.R.layout.simple_spinner_item,
            options
        ).also {a ->
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            acs_fill_type.adapter = a
        }

        acs_fill_type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val localState = currentState
                if (localState is CreateFillViewState.CreatingFill) {
                    FillData.fromFill(localState.fill).copy(
                        type = options[position].type
                    ).let {bsd ->
                        vm?.updateFill(bsd)
                    }
                }
            }

        }
    }

    override fun bindViewModel(vm : CreateFillViewModel) {
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
    override fun setState (state : CreateFillViewState) {
        this.nextState = state
        post { reset() }
    }

    @UiThread
    private fun reset() {
        val localNext = nextState
        val localCurrent = currentState
        if (localNext != localCurrent) {
            when(localNext){
                is CreateFillViewState.CreatingFill -> {
                    if (et_notes.text.toString() != localNext.fill.notes) {
                        et_notes.setText(localNext.fill.notes)
                    }
                    val optionPos = options.indexOfFirst {
                        it.type == localNext.fill.type
                    }
                    val selectedPos = acs_fill_type.selectedItemPosition
                    if (optionPos != selectedPos) {
                        acs_fill_type.setSelection(selectedPos)
                    }

                    Unit
                }
                CreateFillViewState.NoFill -> {
                    et_notes.setText("")
                }
            }.let { currentState = localNext }
        }
    }
}