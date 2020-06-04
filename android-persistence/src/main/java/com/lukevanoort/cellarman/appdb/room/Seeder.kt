package com.lukevanoort.cellarman.appdb.room

import com.lukevanoort.cellarman.fill.model.FillType
import com.lukevanoort.cellarman.units.VolumeMeasurement
import com.lukevanoort.cellarman.units.VolumeUnit
import com.lukevanoort.cellarman.vessel.model.VesselMaterial
import io.reactivex.Observable
import io.reactivex.Scheduler
import java.util.*

internal fun testSeedDB(db: VesselManagementRoomDatabase, scheduler: Scheduler) {
    val ignored = Observable.just(Any())
        .observeOn(scheduler)
        .map {
            val rand = Random(500)

            val v1 = VesselEntity(
                id = UUID(rand.nextLong(), rand.nextLong()),
                name = "Fred Foeder",
                capacity = VolumeMeasurement(30.0, VolumeUnit.Hectoliter),
                material = VesselMaterial.FrenchOak,
                notes = "This is a very fancy foeder"
            )
            val v2 = VesselEntity(
                id = UUID(rand.nextLong(), rand.nextLong()),
                name = "Billy Bourbon",
                capacity = VolumeMeasurement(2.0, VolumeUnit.BeerBarrel),
                material = VesselMaterial.AmericanOak,
                notes = "Ex-Heaven Hill barrel"
            )
            val v3 = VesselEntity(
                id = UUID(rand.nextLong(), rand.nextLong()),
                name = "Carol Corney",
                capacity = VolumeMeasurement(5.5, VolumeUnit.USGallon),
                material = VesselMaterial.StainlessSteel,
                notes = "Has contained pedio"
            )
            db.getVesselDao().insertAll(v1, v2, v3)

            val v1f1 = FillEntity(
                id = UUID(rand.nextLong(), rand.nextLong()),
                notes = "Wild-fermented Gamay",
                time = Date(1483228800),
                type = FillType.Wine,
                vesselId = v2.id
            )

            val v1f2 = FillEntity(
                id = UUID(rand.nextLong(), rand.nextLong()),
                notes = "Spontaneous wild ale",
                time = Date(),
                type = FillType.Beer,
                vesselId = v1.id
            )

            val v2f1 = FillEntity(
                id = UUID(rand.nextLong(), rand.nextLong()),
                notes = "3year aged 60% corn, 40% rye",
                time = Date(1483228800),
                type = FillType.Whisky,
                vesselId = v2.id
            )

            val v2f2 = FillEntity(
                id = UUID(rand.nextLong(), rand.nextLong()),
                notes = "Super Chitty Chitty Bonbon Imperial Stout",
                time = Date(),
                type = FillType.Beer,
                vesselId = v2.id
            )

            val v3f1 = FillEntity(
                id = UUID(rand.nextLong(), rand.nextLong()),
                notes = "Belgian Golden Strong",
                time = Date(),
                type = FillType.Beer,
                vesselId = v2.id
            )

            db.getFillDao().insertAll(v1f1, v1f2, v2f1, v2f2, v3f1)

            val v1s1 = BeerSampleEntity(
                id = UUID(rand.nextLong(), rand.nextLong()),
                vesselId = v1.id,
                fillId = v1f2.id,
                time = Date(),
                notes = "tastes good",
                aceticLevel = 5,
                funkLevel = 3,
                sourLevel = 4,
                ropeLevel = 0,
                oakLevel = 0
            )
            db.getSampleDao().insertAll(v1s1)
        }.subscribe { }
}