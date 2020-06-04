package com.lukevanoort.cellarman.fill.model

import java.util.Date
import java.util.UUID

sealed class FillType {
    object Wine : FillType()
    object Beer : FillType()
    object Gin : FillType()
    object Whisky : FillType()
    object Water : FillType()
    object HoldingSolution : FillType()
    object SulphurDioxide : FillType()
    object Air : FillType()
    object PBW : FillType()
    object StarSan : FillType()
    object Other : FillType()
}

interface Fill {
    abstract val id: UUID
    abstract val vesselId: UUID
    abstract val time: Date
    abstract val type: FillType
    abstract val notes: String
}

data class FillData (
    override val id: UUID,
    override val vesselId: UUID,
    override val time: Date,
    override val type: FillType,
    override val notes: String
) : Fill {
    companion object {
        fun fromFill(fill: Fill) : FillData = when(fill) {
            is FillData -> fill
            else -> FillData(
                id = fill.id,
                vesselId = fill.vesselId,
                time = fill.time,
                type = fill.type,
                notes = fill.notes
            )
        }
    }
}