package com.lukevanoort.cellarman.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.lukevanoort.cellarman.dagger.RootTaskWad
import com.lukevanoort.cellarman.tasks.RootTask
import com.lukevanoort.cellarman.tasks.RootCanvas
import com.lukevanoort.stuntman.SMBackPressedListener
import javax.inject.Inject

class RootActivity : AppCompatActivity() {
    private lateinit var component: ActivityComponent

    @Inject
    lateinit var task: RootTask
    @Inject
    lateinit var canvas: RootCanvas

    @Inject
    @RootTaskWad
    lateinit var rootTaskWadPersistTrigger: WadPersistTrigger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        component = getAppComponent().plusActivity(
            ActivityModule(this)
        )
        component.inject(this)
        task.useCanvas(canvas)

        val localCanvas = canvas
        if (localCanvas is SMBackPressedListener) {
            onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if(!localCanvas.backPressed()) {
                        this@RootActivity.finish()
                    }
                }

            })
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // not going to bother persisting when we're just changing orientations
        if (!isChangingConfigurations) {
            rootTaskWadPersistTrigger.persistWad()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        task.removeCanvas()
    }
}
