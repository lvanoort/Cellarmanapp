package com.lukevanoort.stuntman

import android.graphics.Canvas
import android.view.ViewGroup

interface SMBackPressedListener {
    // back button was pressed, returns true if the
    // event was handled, false if it should bubble back up
    fun backPressed() : Boolean
}

interface SMCanvas {
    fun detachAllViewModels()
}