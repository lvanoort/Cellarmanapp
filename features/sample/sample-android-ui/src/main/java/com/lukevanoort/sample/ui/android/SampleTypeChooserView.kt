package com.lukevanoort.sample.ui.android

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import com.lukevanoort.cellarman.sample.logic.SampleTypeChooserViewModel
import com.lukevanoort.cellarman.sample.logic.SampleTypeChooserViewState
import com.lukevanoort.sample.model.SampleType
import com.lukevanoort.stuntman.SMView
import io.reactivex.disposables.Disposable

class SampleTypeChooserView : AppCompatSpinner,
    SMView<SampleTypeChooserViewState, SampleTypeChooserViewModel> {

    var vm: SampleTypeChooserViewModel? = null
    var vmSub: Disposable? = null

    var currentState: SampleTypeChooserViewState? = null
    var nextState: SampleTypeChooserViewState? = null

    constructor(context: Context) : super(context) { initialize(context) }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { initialize(context) }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) { initialize(context) }

    private sealed class SampleTypeOptions {
        abstract fun getSampleType() : SampleType

        data class Generic(private val ctx: Context) : SampleTypeOptions() {
            override fun toString(): String {
                return ctx.getString(R.string.sampletype_generic)
            }

            override fun getSampleType(): SampleType = SampleType.Generic
        }

        data class Beer(private val ctx: Context) : SampleTypeOptions() {
            override fun toString(): String {
                return ctx.getString(R.string.sampletype_beer)
            }

            override fun getSampleType(): SampleType = SampleType.Beer
        }
    }

    private val options: List<SampleTypeOptions> = listOf<SampleTypeOptions>(
        SampleTypeOptions.Generic(
            context
        ),
        SampleTypeOptions.Beer(
            context
        )
    )

    private fun initialize(ctx: Context) {
        ArrayAdapter(
            ctx,
            android.R.layout.simple_spinner_item,
            options
        ).also {a ->
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            adapter = a
        }

        onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                options[position].let {opt ->
                    when (opt) {
                        is SampleTypeOptions.Generic -> SampleType.Generic
                        is SampleTypeOptions.Beer -> SampleType.Beer
                    }.let {st ->
                        vm?.switchTo(st)
                    }
                }
            }

        }

    }

    override fun bindViewModel(vm: SampleTypeChooserViewModel) {
        unbindViewModel()
        this.vm = vm
        vmSub = vm.getState().subscribe {
            setState(it)
        }
    }

    private fun reset() {
        val localCurrent = currentState
        val localNext = nextState
        if (localCurrent != localNext) {
            when(localNext) {
                is SampleTypeChooserViewState.TypeChosen -> {
                    setSelection(options.indexOfFirst {
                        it.getSampleType() == localNext.sampleType
                    }.coerceAtLeast(0))
                }
                null -> {
                    setSelection(0)
                }
            }

            currentState = localNext
        }
    }

    override fun setState(state: SampleTypeChooserViewState) {
        nextState = state
        post { reset() }
    }

    override fun unbindViewModel() {
        vmSub?.dispose()
        vm = null
    }
}