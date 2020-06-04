package com.lukevanoort.cellarman.app

import android.app.Application
import android.content.Context
import com.lukevanoort.cellarman.dagger.*
import com.lukevanoort.cellarman.stuntman.*
import com.lukevanoort.cellarman.tasks.RootTask
import com.lukevanoort.stuntman.SMMapTaskWad
import com.lukevanoort.stuntman.SMTaskDataWad
import com.lukevanoort.stuntman.SMTaskReadableDataWad
import com.lukevanoort.stuntman.SMTaskWadPersister
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

@Module
class AppModule (private val app: CellarmanApplication) {
    @Provides
    @AppScope
    fun provideApplication(): Application {
        return app
    }

    @Provides
    @AppScope
    @AppContext
    fun provideAppContext(): Context {
        return app.applicationContext
    }

    @Provides
    @AppScope
    @IOScheduler
    fun provideIOScheduler(): Scheduler {
        return Schedulers.io()
    }

    @Provides
    @AppScope
    @UIScheduler
    fun provideUIScheduler(): Scheduler {
        return AndroidSchedulers.mainThread()
    }

    @Provides
    @AppScope
    @RootTaskWad
    fun provideWadPersister(): SMTaskWadPersister {
        return SharedPreferenceGsonWadPersister(
            app.getSharedPreferences(WAD_PERSISTER_KEY,Context.MODE_PRIVATE),
            true
        )
    }

    @Provides
    @AppScope
    @RootTaskWad
    fun provideRootTaskWad(@RootTaskWad persister: SMTaskWadPersister): SMTaskDataWad {
        return persister.readWad()
    }


    @Provides
    @RootTaskWad
    fun provideRootReadableTaskWad(@RootTaskWad wad: SMTaskDataWad): SMTaskReadableDataWad {
        return wad
    }

    @Provides
    @AppScope
    @RootTaskWad
    fun provideRootTaskWadPersistTrigger(rootTask: RootTask, @RootTaskWad persister: SMTaskWadPersister): WadPersistTrigger {
        return object : WadPersistTrigger {
            override fun persistWad() {
                val wad = SMMapTaskWad()
                rootTask.writeWad(wad)
                persister.writeWad(wad)
            }
        }
    }

//    @Provides
//    @AppScope
//    fun providesRootAddress() : HistoricalStackManager<RootTaskAddress> {
//        return TransientStackManager<RootTaskAddress>()
//    }

    companion object {
        private const val WAD_PERSISTER_KEY = "ROOT_WAD_PERSISTER"
    }
}