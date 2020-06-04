package com.lukevanoort.cellarman.appdb.room

import android.content.Context
import androidx.room.Room
import com.lukevanoort.cellarman.dagger.AppContext
import com.lukevanoort.cellarman.dagger.AppScope
import com.lukevanoort.cellarman.dagger.IOScheduler
import com.lukevanoort.cellarman.fill.logic.FillRepository
import com.lukevanoort.cellarman.sample.logic.SampleRepository
import com.lukevanoort.cellarman.vessel.logic.VesselRepository
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler

sealed class AppDBConfiguration {
    data class InMemory(
        val seedDB: Boolean
    ) : AppDBConfiguration()
    object Persistent : AppDBConfiguration()
}

@Module
class RoomVesselManagementModule {
    @Provides
    @AppScope
    fun provideVesselManagementRoomDatabase(
        @AppContext context: Context,
        @IOScheduler scheduler: Scheduler,
        dbCfg: AppDBConfiguration
    ) : VesselManagementRoomDatabase {
        return when(dbCfg) {
            is AppDBConfiguration.InMemory -> {
                Room.inMemoryDatabaseBuilder(context, VesselManagementRoomDatabase::class.java)
                    .build().also {
                        if (dbCfg.seedDB) {
                            testSeedDB(it, scheduler)
                        }
                    }
            }
            AppDBConfiguration.Persistent -> {
                Room.databaseBuilder(
                    context,
                    VesselManagementRoomDatabase::class.java,
                    VesselManagementRoomDatabase.DB_NAME
                ).build()
            }
        }
    }

    @Provides
    fun provideRepository(db: VesselManagementDatabaseAdapter) : VesselManagementRepository = db

    @Provides
    fun provideVesselRepository(db: VesselManagementRepository) : VesselRepository = db

    @Provides
    fun provideFillRepository(db: VesselManagementRepository) : FillRepository = db

    @Provides
    fun provideSampleRepository(db: VesselManagementRepository) : SampleRepository = db

}