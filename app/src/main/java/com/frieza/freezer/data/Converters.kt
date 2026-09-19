package com.frieza.freezer.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromCategory(category: FoodCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): FoodCategory = FoodCategory.valueOf(value)

    @TypeConverter
    fun fromQuantityType(type: QuantityType): String = type.name

    @TypeConverter
    fun toQuantityType(value: String): QuantityType = QuantityType.valueOf(value)
}
