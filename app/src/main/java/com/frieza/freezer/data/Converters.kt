package com.frieza.freezer.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromIds(ids: List<Long>): String = ids.joinToString(",")

    @TypeConverter
    fun toIds(value: String): List<Long> =
        if (value.isBlank()) emptyList() else value.split(",").mapNotNull { it.trim().toLongOrNull() }
}
