package com.lukevanoort.cellarman.vessel.model

import com.lukevanoort.cellarman.units.VolumeMeasurement
import java.util.*

interface Vessel {
    abstract val id: UUID
    abstract val name: String
    abstract val material: VesselMaterial
    abstract val capacity: VolumeMeasurement
    abstract val notes: String
}

data class VesselData(
    override val id: UUID ,
    override val name: String ,
    override val material: VesselMaterial ,
    override val capacity: VolumeMeasurement,
    override val notes: String
) : Vessel {
    companion object {
        fun from(vessel: Vessel) : VesselData = when(vessel) {
            is VesselData -> vessel
            else -> VesselData(
                id = vessel.id,
                name = vessel.name,
                material = vessel.material,
                capacity = vessel.capacity,
                notes = vessel.notes
            )
        }
    }
}

sealed class VesselMaterial {
    object FrenchOak : VesselMaterial()
    object HungarianOak : VesselMaterial()
    object AmericanOak : VesselMaterial()
    object PET : VesselMaterial()
    object StainlessSteel : VesselMaterial()
    object Clay : VesselMaterial()
    object HDPE : VesselMaterial()
    object Other : VesselMaterial()
}