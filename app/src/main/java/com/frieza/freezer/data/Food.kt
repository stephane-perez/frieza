package com.frieza.freezer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: FoodCategory,
    val floor: Int,
    val quantityType: QuantityType = QuantityType.AUCUNE,
    val quantityValue: Int? = null,
    val expirationDate: Long? = null,
    val dateAdded: Long = System.currentTimeMillis()
)
