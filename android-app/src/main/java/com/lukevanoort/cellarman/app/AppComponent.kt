package com.lukevanoort.cellarman.app

import com.lukevanoort.cellarman.dagger.AppScope
import com.lukevanoort.cellarman.appdb.room.RoomVesselManagementModule
import dagger.Component

@AppScope
@Component(
    modules = [AppModule::class, RoomVesselManagementModule::class, FeatureCfgModule::class]
)
interface AppComponent {
    fun inject(app: CellarmanApplication)

    fun plusActivity(activityModule: ActivityModule): ActivityComponent
}