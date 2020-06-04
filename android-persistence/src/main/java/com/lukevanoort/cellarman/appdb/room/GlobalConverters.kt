package com.lukevanoort.cellarman.appdb.room

import androidx.room.TypeConverter
import java.util.Date
import java.util.UUID

class GlobalConverters {
    @TypeConverter
    fun uuidToString(uuid: UUID?): String? = uuid?.toString()
    @TypeConverter
    fun stringToUuid(string: String?): UUID? = string?.let { UUID.fromString(it) }

    @TypeConverter
    fun instantToLong(date: Date): Long = date.time
    @TypeConverter
    fun longToInstant(long: Long): Date = Date(long)

}