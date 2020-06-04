package com.lukevanoort.cellarman.vessel.logic

import com.lukevanoort.cellarman.util.MaybeResult
import com.lukevanoort.cellarman.util.SuccessIndicator
import com.lukevanoort.cellarman.vessel.model.Vessel
import io.reactivex.Observable
import java.util.UUID

interface VesselRepository {
    fun getAllVessels() : Observable<out List<Vessel>>
    fun getVessel(id: UUID) : Observable<out MaybeResult<Vessel>>
    fun addVessel(vessel: Vessel) : com.lukevanoort.cellarman.util.SuccessIndicator
    fun updateVessel(vessel: Vessel) : com.lukevanoort.cellarman.util.SuccessIndicator
    fun deleteVessel(vessel: Vessel) : com.lukevanoort.cellarman.util.SuccessIndicator
}