package com.lukevanoort.sample.ui.android

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import com.lukevanoort.cellarman.sample.logic.CreateBeerSampleViewModel
import com.lukevanoort.cellarman.sample.logic.CreateBeerSampleViewState
import com.lukevanoort.sample.model.BeerSampleData
import com.lukevanoort.stuntman.SMView
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.sample_create_beer_layout.view.acb_cancel
import kotlinx.android.synthetic.main.sample_create_beer_layout.view.acb_save
import kotlinx.android.synthetic.main.sample_create_beer_layout.view.et_notes


class BeerSampleDataView : ConstraintLayout,
    SMView<CreateBeerSampleViewState, CreateBeerSampleViewModel> {
    private var currentState : CreateBeerSampleViewState = CreateBeerSampleViewState.NoSample
    private var nextState : CreateBeerSampleViewState = CreateBeerSampleViewState.NoSample

    private var vm : CreateBeerSampleViewModel? = null
    private var disp : Disposable? = null

    constructor(context: Context) : super(context) { initialize(context) }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { initialize(context) }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) { initialize(context) }

    private fun initialize(ctx: Context) {
        LayoutInflater.from(ctx).inflate(R.layout.sample_create_beer_layout,this)
        et_notes.addTextChangedListener { et ->
            val localState = currentState
            if (localState is CreateBeerSampleViewState.CreatingBeerSample) {
                BeerSampleData.fromSample(localState.sample).copy(
                    notes = et.toString()
                ).let {bsd ->
                    vm?.updateSample(bsd)
                }
            }
        }
        acb_save.setOnClickListener {
            val localState = currentState
            if (localState is CreateBeerSampleViewState.CreatingBeerSample) {
                vm?.createBeerSample(localState.sample)
            }
        }

        acb_cancel.setOnClickListener {
            vm?.cancel()
        }
    }

    override fun bindViewModel(vm : CreateBeerSampleViewModel) {
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
    override fun setState (state : CreateBeerSampleViewState) {
        this.nextState = state
        post { reset() }
    }

    @UiThread
    private fun reset() {
        val localNext = nextState
        val localCurrent = currentState
        if (localNext != localCurrent) {
            when(localNext){
                is CreateBeerSampleViewState.CreatingBeerSample -> {
                    if (et_notes.text.toString() != localNext.sample.notes) {
                        et_notes.setText(localNext.sample.notes)
                    } else {
                        Unit
                    }
                }
                CreateBeerSampleViewState.NoSample -> {
                    et_notes.setText("")
                }
            }.let { currentState = localNext }
        }
    }
}