package com.lukevanoort.cellarman.sample.logic

import com.lukevanoort.cellarman.fill.model.Fill
import com.lukevanoort.cellarman.util.MaybeResult
import com.lukevanoort.cellarman.util.SuccessIndicator
import com.lukevanoort.sample.model.Sample
import io.reactivex.Observable
import java.util.UUID

interface SampleRepository {
    fun getSample(id: UUID) : Observable<out MaybeResult<Sample>>
    fun getVesselSampleHistory(vesselID: UUID) : Observable<out List<Sample>>
    fun getVesselFillHistory(vesselID: UUID) : Observable<out List<Fill>>
    fun getFillSampleHistory(fillID: UUID) : Observable<out List<Sample>>
    fun getSampleHistory() : Observable<out List<Sample>>
    fun recordSample(sample: Sample) : com.lukevanoort.cellarman.util.SuccessIndicator
    fun updateSample(sample: Sample) : com.lukevanoort.cellarman.util.SuccessIndicator
}