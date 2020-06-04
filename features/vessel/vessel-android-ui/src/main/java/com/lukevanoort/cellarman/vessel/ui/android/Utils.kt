package com.lukevanoort.cellarman.vessel.ui.android

import android.content.Context
import com.lukevanoort.cellarman.units.VolumeMeasurement
import com.lukevanoort.cellarman.units.VolumeUnit
import com.lukevanoort.cellarman.vessel.model.Vessel
import com.lukevanoort.cellarman.vessel.model.VesselMaterial

fun VolumeUnit.toDisplayAbbreviation(ctx: Context) : String = when(this) {
        VolumeUnit.Hectoliter -> ctx.getString(R.string.abbrev_hectoliter)
        VolumeUnit.BeerBarrel -> ctx.getString(R.string.abbrev_beer_barrel)
        VolumeUnit.Liter -> ctx.getString(R.string.abbrev_liter)
        VolumeUnit.USGallon -> ctx.getString(R.string.abbrev_us_gal)
}

fun VolumeMeasurement.toShortDescription(ctx: Context) : String = ctx.getString(
    R.string.short_capacity_description,
    this.amount,
    this.unit.toDisplayAbbreviation(ctx)
)

fun VesselMaterial.toDisplayName(ctx: Context) : String = when(this){
        VesselMaterial.FrenchOak -> ctx.getString(R.string.name_french_oak)
        VesselMaterial.HungarianOak -> ctx.getString(R.string.name_hungarian_oak)
        VesselMaterial.AmericanOak -> ctx.getString(R.string.name_american_oak)
        VesselMaterial.PET -> ctx.getString(R.string.name_pet)
        VesselMaterial.StainlessSteel -> ctx.getString(R.string.name_stainless_steel)
        VesselMaterial.Clay -> ctx.getString(R.string.name_clay)
        VesselMaterial.HDPE -> ctx.getString(R.string.name_hdpe)
        VesselMaterial.Other -> ctx.getString(R.string.name_other)
    }


fun Vessel.getConstructionShortSummary(ctx: Context) : String = ctx.getString(
    R.string.cap_and_material_summary,
    this.capacity.toShortDescription(ctx),
    this.material.toDisplayName(ctx)
)