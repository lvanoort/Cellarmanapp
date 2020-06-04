package com.lukevanoort.cellarman.appdb.room

import androidx.room.*
import com.lukevanoort.sample.model.BeerSample
import com.lukevanoort.sample.model.GenericSample
import com.lukevanoort.sample.model.Sample
import com.lukevanoort.sample.model.SampleType
import io.reactivex.Observable
import java.lang.IllegalArgumentException
import java.util.Date
import java.util.UUID

@Entity(tableName = "generic_sample_entitiy")
class GenericSampleEntity(
    @PrimaryKey
    override val id: UUID,
    @ColumnInfo(name="fill_id")
    override val fillId: UUID?,
    @ColumnInfo(name="vessel_id")
    override val vesselId: UUID,
    override val time: Date,
    override val notes: String
) : GenericSample {
    companion object {
        fun fromGenericSample(sample: GenericSample) : GenericSampleEntity = GenericSampleEntity(
            id = sample.id,
            fillId = sample.fillId,
            vesselId = sample.vesselId,
            time = sample.time,
            notes = sample.notes
        )
    }
}

@Entity(tableName = "beer_sample_entitiy")
class BeerSampleEntity(
    @PrimaryKey
    override val id: UUID,
    @ColumnInfo(name="fill_id")
    override val fillId: UUID?,
    @ColumnInfo(name="vessel_id")
    override val vesselId: UUID,
    override val time: Date,
    override val notes: String,

    @ColumnInfo(name="acetic_level")
    override val aceticLevel: Int? = 0,
    @ColumnInfo(name="funk_level")
    override val funkLevel: Int? = 0,
    @ColumnInfo(name="sour_level")
    override val sourLevel: Int? = 0,
    @ColumnInfo(name="rope_level")
    override val ropeLevel: Int? = 0,
    @ColumnInfo(name="oak_level")
    override val oakLevel: Int? = 0
) : BeerSample {
    companion object {
        fun fromBeerSample(sample: BeerSample) : BeerSampleEntity = BeerSampleEntity(
            id = sample.id,
            vesselId = sample.vesselId,
            fillId = sample.fillId,
            time = sample.time,
            notes = sample.notes,
            aceticLevel = sample.aceticLevel,
            funkLevel = sample.funkLevel,
            sourLevel = sample.sourLevel,
            ropeLevel = sample.ropeLevel,
            oakLevel = sample.oakLevel
        )
    }
}


class SampleConverters {
    @TypeConverter
    fun sampleTypeFromString(string: String) : SampleType = when(string) {
        "beer" -> SampleType.Beer
        "generic" -> SampleType.Generic
        else -> {
            throw IllegalArgumentException("unable to support sample type")
        }
    }
}

@DatabaseView(
    value = "SELECT id, fill_id, vessel_id, time, notes, 'generic' AS sample_type FROM generic_sample_entitiy " +
            "UNION ALL SELECT id, fill_id, vessel_id, time, notes, 'beer' AS sample_type FROM beer_sample_entitiy",
    viewName="all_sample_view"
)
class AllSampleView(
    @PrimaryKey
    override val id: UUID,
    @ColumnInfo(name="fill_id")
    override val fillId: UUID?,
    @ColumnInfo(name="vessel_id")
    override val vesselId: UUID,
    override val time: Date,
    override val notes: String,
    @ColumnInfo(name="sample_type")
    override val sampleType: SampleType
) : Sample

@Dao
interface SampleDao {
    @Query("SELECT * FROM beer_sample_entitiy WHERE id=:sampleId ORDER BY time DESC")
    fun getBeerSample(sampleId: UUID): Observable<List<BeerSampleEntity>>

    @Query("SELECT * FROM generic_sample_entitiy WHERE id=:sampleId ORDER BY time DESC")
    fun getGenericSample(sampleId: UUID): Observable<List<GenericSampleEntity>>

    @Query("SELECT * FROM all_sample_view WHERE vessel_id=:vesselId ORDER BY time DESC")
    fun getAllSamplesForVessel(vesselId: UUID): Observable<List<AllSampleView>>

    @Query("SELECT * FROM all_sample_view WHERE fill_id=:fillId ORDER BY time DESC")
    fun getAllSamplesForFill(fillId: UUID): Observable<List<AllSampleView>>

    @Query("SELECT * FROM all_sample_view WHERE id=:sampleId ORDER BY time DESC")
    fun getAllSampleView(sampleId: UUID): Observable<List<AllSampleView>>

    @Query("SELECT * FROM all_sample_view ORDER BY time DESC")
    fun getAllSampleView(): Observable<List<AllSampleView>>

    @Insert
    fun insertAll(vararg entites: BeerSampleEntity)

    @Update
    fun updateSample(entity: BeerSampleEntity)

    @Delete
    fun delete(entity: BeerSampleEntity)


    @Insert
    fun insertAll(vararg entites: GenericSampleEntity)

    @Update
    fun updateSample(entity: GenericSampleEntity)

    @Delete
    fun delete(entity: GenericSampleEntity)
}


