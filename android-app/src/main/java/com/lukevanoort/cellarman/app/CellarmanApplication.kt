package com.lukevanoort.cellarman.app

import android.app.Application
import android.content.Context

class CellarmanApplication : Application() {
    lateinit var component: AppComponent

    override fun onCreate() {
        super.onCreate()

        component = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()

        component.inject(this)
    }


}


fun Context.getAppComponent(): AppComponent {
    return (this.applicationContext as CellarmanApplication).component
}