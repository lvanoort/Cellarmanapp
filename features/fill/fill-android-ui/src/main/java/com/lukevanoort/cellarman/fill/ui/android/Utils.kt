package com.lukevanoort.cellarman.fill.ui.android

import android.content.Context
import com.lukevanoort.cellarman.fill.model.FillType

fun FillType.toDisplayName(ctx: Context) : String = ctx.getString(when(this) {
    FillType.Wine -> R.string.filltype_wine
    FillType.Beer -> R.string.filltype_beer
    FillType.Gin -> R.string.filltype_gin
    FillType.Whisky -> R.string.filltype_whisky
    FillType.Water -> R.string.filltype_water
    FillType.HoldingSolution -> R.string.filltype_holding_solution
    FillType.SulphurDioxide -> R.string.filltype_sulphur_dioxide
    FillType.Air -> R.string.filltype_air
    FillType.PBW -> R.string.filltype_pbw
    FillType.StarSan -> R.string.filltype_starsan
    FillType.Other -> R.string.filltype_other
})