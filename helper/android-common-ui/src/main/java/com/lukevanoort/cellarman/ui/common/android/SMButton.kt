package com.lukevanoort.cellarman.ui.common.android

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatButton
import com.lukevanoort.stuntman.SMView
import com.lukevanoort.stuntman.SimpleButtonState
import com.lukevanoort.stuntman.SimpleButtonVM
import io.reactivex.disposables.Disposable

class SMButton : AppCompatButton,
    SMView<SimpleButtonState, SimpleButtonVM> {
    private var currentState : SimpleButtonState = SimpleButtonState.Enabled
    private var nextState : SimpleButtonState = SimpleButtonState.Enabled

    private var vm : SimpleButtonVM? = null
    private var disp : Disposable? = null

    constructor(context: Context) : super(context) { initialize(context) }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { initialize(context) }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) { initialize(context) }

    private fun initialize(context: Context) {
        setOnClickListener {
            vm?.buttonPressed()
        }
    }

    override fun bindViewModel(vm: SimpleButtonVM) {
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
    override fun setState(state: SimpleButtonState) {
        nextState = state
        post { reset() }
    }

    @UiThread
    private fun reset() {
        val localNext = nextState
        val localCurrent = currentState
        if (localNext != localCurrent) {
            when(localNext) {
                SimpleButtonState.Enabled -> {
                    this.isEnabled = true
                }
                SimpleButtonState.Disabled -> {
                    this.isEnabled = false
                }
            }.let { currentState = localNext }
        }
    }
}