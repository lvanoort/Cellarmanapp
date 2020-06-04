package com.lukevanoort.cellarman.app

import com.lukevanoort.cellarman.dagger.ActivityScope
import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = [ActivityModule::class])
interface ActivityComponent {
    // activity injection
    fun inject(act: RootActivity)
}