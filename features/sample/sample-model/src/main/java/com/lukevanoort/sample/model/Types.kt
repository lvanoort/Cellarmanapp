package com.lukevanoort.sample.model

import java.util.Date
import java.util.UUID

sealed class SampleType {
    object Beer : SampleType()
    object Generic: SampleType()
}

interface Sample {
    abstract val id: UUID
    abstract val vesselId: UUID
    abstract val fillId: UUID?
    abstract val time: Date
    abstract val notes: String
    abstract val sampleType: SampleType
}


interface GenericSample : Sample {
    override val sampleType: SampleType
    get() = SampleType.Generic
    companion object {
        fun fromSample(sample: Sample) : GenericSample = when(sample) {
            is GenericSample -> {
                sample
            }
            else -> {
                GenericSampleData(
                    id = sample.id,
                    vesselId = sample.vesselId,
                    fillId = sample.fillId,
                    time = sample.time,
                    notes = sample.notes
                )
            }
        }
    }
}

interface BeerSample : Sample {
    abstract val aceticLevel: Int?
    abstract val funkLevel: Int?
    abstract val sourLevel: Int?
    abstract val ropeLevel: Int?
    abstract val oakLevel: Int?
    override val sampleType: SampleType
        get() = SampleType.Beer

    companion object {
        fun fromSample(sample: Sample) : BeerSample = when(sample) {
            is BeerSample -> {
                sample
            }
            else -> {
                BeerSampleData(
                    id = sample.id,
                    vesselId = sample.vesselId,
                    fillId = sample.fillId,
                    time = sample.time,
                    notes = sample.notes,
                    aceticLevel = null,
                    funkLevel = null,
                    sourLevel = null,
                    ropeLevel = null,
                    oakLevel = null
                )
            }
        }
    }
}


data class GenericSampleData(
    override val id: UUID,
    override val vesselId: UUID,
    override val fillId: UUID?,
    override val time: Date,
    override val notes: String
) : GenericSample {
    companion object {
        fun fromSample(sample: Sample) : GenericSampleData = when(sample) {
            is GenericSampleData -> {
                sample
            }
            is GenericSample -> {
                GenericSampleData(
                    id = sample.id,
                    vesselId = sample.vesselId,
                    fillId = sample.fillId,
                    time = sample.time,
                    notes = sample.notes
                )
            }
            else -> {
                GenericSampleData(
                    id = sample.id,
                    vesselId = sample.vesselId,
                    fillId = sample.fillId,
                    time = sample.time,
                    notes = sample.notes
                )
            }
        }
    }
}

data class BeerSampleData(
    override val id: UUID,
    override val vesselId: UUID,
    override val fillId: UUID?,
    override val time: Date,
    override val notes: String,
    override val aceticLevel: Int?,
    override val funkLevel: Int?,
    override val sourLevel: Int?,
    override val ropeLevel: Int?,
    override val oakLevel: Int?
) : BeerSample {
    companion object {
        fun fromSample(sample: Sample) : BeerSampleData = when(sample) {
            is BeerSampleData -> {
                sample
            }
            is BeerSample -> {
                BeerSampleData(
                    id = sample.id,
                    vesselId = sample.vesselId,
                    fillId = sample.fillId,
                    time = sample.time,
                    notes = sample.notes,
                    aceticLevel = sample.aceticLevel,
                    funkLevel = sample.funkLevel,
                    sourLevel = sample.sourLevel,
                    ropeLevel = sample.ropeLevel,
                    oakLevel = sample.oakLevel
                )
            }
            else -> {
                BeerSampleData(
                    id = sample.id,
                    vesselId = sample.vesselId,
                    fillId = sample.fillId,
                    time = sample.time,
                    notes = sample.notes,
                    aceticLevel = null,
                    funkLevel = null,
                    sourLevel = null,
                    ropeLevel = null,
                    oakLevel = null
                )
            }
        }
    }
}