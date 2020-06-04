package com.lukevanoort.cellarman.appdb.room

import androidx.room.*
import com.lukevanoort.cellarman.units.VolumeMeasurement
import com.lukevanoort.cellarman.units.VolumeUnit
import com.lukevanoort.cellarman.vessel.model.Vessel
import com.lukevanoort.cellarman.vessel.model.VesselMaterial
import io.reactivex.Observable
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.util.UUID

@Entity(tableName = "vessel_entity")
class VesselEntity(
    @PrimaryKey
    override val id: UUID,
    override val name: String,
    override val material: VesselMaterial,
    override val capacity: VolumeMeasurement,
    override val notes: String
) : Vessel {
    companion object{
        fun fromVessel(vessel:Vessel) = VesselEntity(
            id = vessel.id,
            name = vessel.name,
            material = vessel.material,
            capacity = vessel.capacity,
            notes = vessel.notes
        )
    }
}

@Dao
interface VesselDao {
    @Query("SELECT * FROM vessel_entity WHERE id=:vesselId")
    fun getVessel(vesselId: UUID): Observable<List<VesselEntity>>


    @Query("SELECT * FROM vessel_entity ORDER BY name ASC, id ASC")
    fun getAllVessels(): Observable<List<VesselEntity>>

    @Insert
    fun insertAll(vararg entites: VesselEntity)

    @Update
    fun updateVessel(entity: VesselEntity)

    @Delete
    fun delete(entity: VesselEntity)
}


class VesselConverters {

    /*
    The prefix 'X:' in all of these denotes a backwards-incompatible version change, so if
    we have to change this format to something different, we can increment the  number so
    the converter can know what to do when it encounters older format data
     */

    @TypeConverter
    fun materialToString(material: VesselMaterial): String = when(material) {
        VesselMaterial.FrenchOak -> "0:frenchoak"
        VesselMaterial.HungarianOak -> "0:hungarianoak"
        VesselMaterial.AmericanOak -> "0:americanoak"
        VesselMaterial.PET -> "0:pet"
        VesselMaterial.StainlessSteel -> "0:stainless"
        VesselMaterial.Clay -> "0:clay"
        VesselMaterial.HDPE -> "0:hdpe"
        VesselMaterial.Other -> "0:other"
    }

    @TypeConverter
    fun stringToMaterial(material: String): VesselMaterial = when(material) {
        "0:frenchoak" -> VesselMaterial.FrenchOak
        "0:hungarianoak" -> VesselMaterial.HungarianOak
        "0:americanoak" -> VesselMaterial.AmericanOak
        "0:pet" -> VesselMaterial.PET
        "0:stainless" -> VesselMaterial.StainlessSteel
        "0:clay" -> VesselMaterial.Clay
        "0:hdpe" -> VesselMaterial.HDPE
        "0:other" -> VesselMaterial.Other
        else -> VesselMaterial.Other
    }

    @TypeConverter
    fun capacityToString(capacity: VolumeMeasurement): String {
        val builder = StringBuilder(10)
        builder.append('0')
        builder.append(':')
        builder.append(capacity.amount.toString())
        builder.append(':')
        builder.append(when(capacity.unit){
            VolumeUnit.Hectoliter -> "hl"
            VolumeUnit.BeerBarrel -> "bbl"
            VolumeUnit.Liter -> "l"
            VolumeUnit.USGallon -> "usg"
        })
        return builder.toString()
    }

    @TypeConverter
    fun stringToCapacity(string: String): VolumeMeasurement {
        val portions = string.split(':')
        if (portions.size != 3) {
            throw IllegalArgumentException("wrong size for a volume measurement")
        } else {
            return VolumeMeasurement(
                amount = portions[1].toDouble(),
                unit = when(portions[2]) {
                    "hl" -> VolumeUnit.Hectoliter
                    "bbl" -> VolumeUnit.BeerBarrel
                    "l" -> VolumeUnit.Liter
                    "usg" -> VolumeUnit.USGallon
                    else -> {
                        throw IllegalArgumentException(String.format("invalid volume unit: %s",portions[2]))
                    }
                }
            )
        }
    }


}