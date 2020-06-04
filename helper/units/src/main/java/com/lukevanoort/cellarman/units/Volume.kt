package com.lukevanoort.cellarman.units

data class VolumeMeasurement(val amount: Double,val unit: VolumeUnit)

sealed class VolumeUnit {
    object Hectoliter :  VolumeUnit()
    object BeerBarrel : VolumeUnit()
    object Liter : VolumeUnit()
    object USGallon : VolumeUnit()
}