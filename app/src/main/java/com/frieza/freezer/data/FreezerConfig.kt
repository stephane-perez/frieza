package com.frieza.freezer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Singleton row (id is always 0) holding the freezer's structure. */
@Entity(tableName = "freezer_config")
data class FreezerConfig(
    @PrimaryKey val id: Int = 0,
    val floorCount: Int
)
