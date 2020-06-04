package com.lukevanoort.stuntman

import io.reactivex.Observable

interface SMTask<in TaskCanvas : SMCanvas, out OutputEvents : Any> {
    abstract fun getOutput() : Observable<out OutputEvents>

    /**
     * useCanvas detaches existing canvas if present and attaches a new one.
     * On Android, this should be called on the UI thread
     */
    abstract fun useCanvas(c : TaskCanvas)

    /**
     * removeCanvas removes the canvas. On Android, this should be called on the UI thread
     */
    abstract fun removeCanvas()

    /**
     * writeWad is called when the task thinks it may need to be persisted to disk
     */
    fun writeWad(wad: SMTaskWritableDataWad) {}
    /**
     * finished should be called when a Task is done and no longer needed
     * after it is called, the Task instance should not be reused
     */
    abstract fun finished()
}
