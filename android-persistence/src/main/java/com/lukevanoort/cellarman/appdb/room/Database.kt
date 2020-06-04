package com.lukevanoort.cellarman.appdb.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lukevanoort.cellarman.fill.logic.FillRepository
import com.lukevanoort.cellarman.fill.model.Fill
import com.lukevanoort.cellarman.sample.logic.SampleRepository
import com.lukevanoort.cellarman.util.MaybeResult
import com.lukevanoort.cellarman.vessel.logic.VesselRepository
import com.lukevanoort.cellarman.vessel.model.Vessel
import com.lukevanoort.sample.model.BeerSample
import com.lukevanoort.sample.model.GenericSample
import com.lukevanoort.sample.model.Sample
import com.lukevanoort.sample.model.SampleType
import io.reactivex.Observable
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.UUID
import javax.inject.Inject

fun <T : Any> convertListToMaybe(results: List<T>) : MaybeResult<T> = if (results.isEmpty()) {
    MaybeResult.NoResult<T>()
} else {
    MaybeResult.HasResult<T>(results[0])
}

interface VesselManagementRepository : FillRepository, VesselRepository, SampleRepository

@Database(
    entities = [
        VesselEntity::class,
        FillEntity::class,
        GenericSampleEntity::class,
        BeerSampleEntity::class],
    views = [AllSampleView::class],
    version = 1
)
@TypeConverters(GlobalConverters::class, VesselConverters::class, FillConverters::class, SampleConverters::class)
abstract class VesselManagementRoomDatabase : RoomDatabase() {
    abstract fun getVesselDao(): VesselDao
    abstract fun getFillDao(): FillDao
    abstract fun getSampleDao(): SampleDao

    companion object {
        const val DB_NAME = "cellarmandb"
    }
}


class VesselManagementDatabaseAdapter @Inject constructor(val db: VesselManagementRoomDatabase) :
    VesselManagementRepository {
    override fun getAllVessels(): Observable<out List<Vessel>> = db.getVesselDao().getAllVessels()

    override fun getVessel(id: UUID): Observable<out MaybeResult<Vessel>> =
        db.getVesselDao().getVessel(id).map {
            if (it.isEmpty()) {
                MaybeResult.NoResult<Vessel>()
            } else {
                MaybeResult.HasResult(it[0])
            }
        }

    override fun getVesselSampleHistory(id: UUID): Observable<out List<Sample>> {
        return db.getSampleDao().getAllSamplesForVessel(id)
    }

    override fun getFillSampleHistory(fillID: UUID): Observable<out List<Sample>> {
        return db.getSampleDao().getAllSamplesForFill(fillID)
    }

    override fun getSampleHistory(): Observable<out List<Sample>> {
        return db.getSampleDao().getAllSampleView()
    }

    override fun getSample(id: UUID): Observable<out MaybeResult<Sample>> {
        return db.getSampleDao().getAllSampleView(id).switchMap { asvs ->
            if (asvs.isEmpty()) {
                Observable.just(MaybeResult.NoResult<Sample>())
            } else {
                val sample = asvs[0]
                when (sample.sampleType) {
                    is SampleType.Beer -> {
                        db.getSampleDao().getBeerSample(id).map { convertListToMaybe(it) }
                    }
                    is SampleType.Generic -> {
                        db.getSampleDao().getGenericSample(id).map { convertListToMaybe(it) }
                    }
                }
            }
        }
    }

    override fun getVesselFillHistory(vesselID: UUID): Observable<out List<Fill>> =
        db.getFillDao().getFillsForVessel(vesselID)

    override fun addVessel(vessel: Vessel): com.lukevanoort.cellarman.util.SuccessIndicator {
        return try {
            db.getVesselDao().insertAll(VesselEntity.fromVessel(vessel))
            com.lukevanoort.cellarman.util.SuccessIndicator.Success
        } catch (e : Exception) {
            com.lukevanoort.cellarman.util.SuccessIndicator.Failure(e)
        }
    }

    override fun updateVessel(vessel: Vessel): com.lukevanoort.cellarman.util.SuccessIndicator {
        return try {
            db.getVesselDao().updateVessel(VesselEntity.fromVessel(vessel))
            com.lukevanoort.cellarman.util.SuccessIndicator.Success
        } catch (e : Exception) {
            com.lukevanoort.cellarman.util.SuccessIndicator.Failure(e)
        }
    }

    override fun recordSample(sample: Sample): com.lukevanoort.cellarman.util.SuccessIndicator {
        return when(sample) {
            is BeerSample -> {
                db.getSampleDao().insertAll(
                    BeerSampleEntity.fromBeerSample(sample)
                )
                com.lukevanoort.cellarman.util.SuccessIndicator.Success
            }
            is GenericSample -> {
                db.getSampleDao().insertAll(
                    GenericSampleEntity.fromGenericSample(sample)
                )
                com.lukevanoort.cellarman.util.SuccessIndicator.Success
            }
            else -> {
                com.lukevanoort.cellarman.util.SuccessIndicator.Failure(reason = IllegalArgumentException("unsupported sample type"))
            }
        }
    }

    override fun updateSample(sample: Sample): com.lukevanoort.cellarman.util.SuccessIndicator {
        return when(sample) {
            is BeerSample -> {
                db.getSampleDao().updateSample(
                    BeerSampleEntity.fromBeerSample(sample)
                )
                com.lukevanoort.cellarman.util.SuccessIndicator.Success
            }
            is GenericSample -> {
                db.getSampleDao().updateSample(
                    GenericSampleEntity.fromGenericSample(sample)
                )
                com.lukevanoort.cellarman.util.SuccessIndicator.Success
            }
            else -> {
                com.lukevanoort.cellarman.util.SuccessIndicator.Failure(reason = IllegalArgumentException("unsupported sample type"))
            }
        }
    }

    override fun getFill(id: UUID): Observable<out MaybeResult<Fill>> {
        return db.getFillDao().getFill(id).map { convertListToMaybe(it) }
    }

    override fun recordFill(fill: Fill): com.lukevanoort.cellarman.util.SuccessIndicator {
        return try {
            db.getFillDao().insertAll(FillEntity.fromFill(fill))
            com.lukevanoort.cellarman.util.SuccessIndicator.Success
        } catch (e : Exception) {
            com.lukevanoort.cellarman.util.SuccessIndicator.Failure(e)
        }
    }

    override fun updateFill(fill: Fill): com.lukevanoort.cellarman.util.SuccessIndicator {
        return try {
            db.getFillDao().update(FillEntity.fromFill(fill))
            com.lukevanoort.cellarman.util.SuccessIndicator.Success
        } catch (e : Exception) {
            com.lukevanoort.cellarman.util.SuccessIndicator.Failure(e)
        }
    }

    override fun getFillHistory(): Observable<out List<Fill>> = db.getFillDao().getFills()

    override fun deleteVessel(vessel: Vessel): com.lukevanoort.cellarman.util.SuccessIndicator {
        return try {
            db.getVesselDao().delete(VesselEntity.fromVessel(vessel))
            com.lukevanoort.cellarman.util.SuccessIndicator.Success
        } catch (e: Exception) {
            com.lukevanoort.cellarman.util.SuccessIndicator.Failure(e)
        }
    }
}