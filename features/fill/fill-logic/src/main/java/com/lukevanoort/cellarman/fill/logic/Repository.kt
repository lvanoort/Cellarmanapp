package com.lukevanoort.cellarman.fill.logic

import com.lukevanoort.cellarman.fill.model.Fill
import com.lukevanoort.cellarman.util.MaybeResult
import com.lukevanoort.cellarman.util.SuccessIndicator
import io.reactivex.Observable
import java.util.UUID

interface FillRepository {
    fun getFill(id: UUID) : Observable<out MaybeResult<Fill>>
    fun getVesselFillHistory(vesselID: UUID) : Observable<out List<Fill>>
    fun getFillHistory() : Observable<out List<Fill>>
    fun recordFill(fill: Fill) : SuccessIndicator
    fun updateFill(fill: Fill) : SuccessIndicator
}