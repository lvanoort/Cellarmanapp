package com.lukevanoort.cellarman.app

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.lukevanoort.cellarman.app.canvases.RootCanvasFactory
import com.lukevanoort.cellarman.dagger.ActivityContext
import com.lukevanoort.cellarman.dagger.ActivityScope
import com.lukevanoort.cellarman.tasks.RootCanvas
import com.lukevanoort.cellarman.tasks.RootTaskAddress
import dagger.Module
import dagger.Provides

@Module
class ActivityModule(
    private val activity: AppCompatActivity
) {
    @Provides
    @ActivityScope
    fun getActivity(): Activity {
        return activity
    }

    @Provides
    @ActivityScope
    @ActivityContext
    fun getActivityContext() : Context {
        return activity
    }

    @get:ActivityScope
    @get:Provides
    val lifecycleOwner: LifecycleOwner
        get() = activity

    @Provides
    @ActivityScope
    fun getRootCanvas(factory: RootCanvasFactory) : RootCanvas {
        return factory.createRootCanvas(activity)
    }
}
