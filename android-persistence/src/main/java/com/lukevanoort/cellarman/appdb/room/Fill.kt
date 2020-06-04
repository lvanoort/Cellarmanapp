package com.lukevanoort.cellarman.appdb.room

import androidx.room.*
import com.lukevanoort.cellarman.fill.model.Fill
import com.lukevanoort.cellarman.fill.model.FillType
import io.reactivex.Observable
import java.util.Date
import java.util.UUID

@Entity(tableName = "fill_entity")
class FillEntity(
    @PrimaryKey
    override val id: UUID,
    @ColumnInfo(name="vessel_id")
    override val vesselId: UUID,
    override val time: Date,
    override val type: FillType,
    override val notes: String
) : Fill {
    companion object {
        fun fromFill(fill: Fill) = FillEntity(
            id = fill.id,
            vesselId = fill.vesselId,
            time = fill.time,
            type = fill.type,
            notes = fill.notes
        )
    }
}

@Dao
interface FillDao {
    @Query("SELECT * FROM fill_entity WHERE id=:fillId")
    fun getFill(fillId: UUID): Observable<List<FillEntity>>

    @Query("SELECT * FROM fill_entity WHERE vessel_id=:vesselId ORDER BY time DESC")
    fun getFillsForVessel(vesselId: UUID): Observable<List<FillEntity>>

    @Query("SELECT * FROM fill_entity")
    fun getFills(): Observable<List<FillEntity>>

    @Insert
    fun insertAll(vararg entites: FillEntity)

    @Delete
    fun delete(entity: FillEntity)

    @Update
    fun update(entity: FillEntity)
}


class FillConverters {

    /*
    The prefix 'X:' in all of these denotes a backwards-incompatible version change, so if
    we have to change this format to something different, we can increment the  number so
    the converter can know what to do when it encounters older format data
     */
    @TypeConverter
    fun fillToString(type: FillType): String = when(type) {
        FillType.Wine -> "0:wine"
        FillType.Beer -> "0:beer"
        FillType.Gin -> "0:gin"
        FillType.Whisky -> "0:whisky"
        FillType.Water -> "0:water"
        FillType.HoldingSolution -> "0:holding"
        FillType.SulphurDioxide -> "0:sulphurdioxide"
        FillType.Air -> "0:air"
        FillType.PBW -> "0:pbw"
        FillType.StarSan -> "0:starsan"
        FillType.Other -> "0:other"
    }

    @TypeConverter
    fun stringToFill(type: String): FillType = when(type) {
        "0:wine"-> FillType.Wine
        "0:beer" -> FillType.Beer
        "0:gin" -> FillType.Gin
        "0:whisky" -> FillType.Whisky
        "0:water" -> FillType.Water
        "0:holding" -> FillType.HoldingSolution
        "0:sulphurdioxide" -> FillType.SulphurDioxide
        "0:air" -> FillType.Air
        "0:pbw" -> FillType.PBW
        "0:starsan" -> FillType.StarSan
        "0:other" -> FillType.Other
        else -> FillType.Other
    }
}