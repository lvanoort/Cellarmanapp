package com.lukevanoort.cellarman.app

import com.lukevanoort.cellarman.appdb.room.AppDBConfiguration
import dagger.Module
import dagger.Provides
import dagger.Reusable

@Module
class FeatureCfgModule {
    @Provides
    @Reusable
    fun provideDbCfg() : AppDBConfiguration {
        return AppDBConfiguration.InMemory(
            true
        )
    }
}