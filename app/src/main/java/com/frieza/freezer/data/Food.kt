package com.frieza.freezer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "freezers")
data class Freezer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int = 0
)

/** A drawer (shelf) of a freezer. [position] 0 is the top drawer. */
@Entity(tableName = "drawers")
data class Drawer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val freezerId: Long,
    val name: String,
    val position: Int = 0
)

/** User-editable category. [color] is an ARGB value (e.g. 0xFF66BB6A). */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long,
    val position: Int = 0
)

@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val drawerId: Long,
    val categoryIds: List<Long> = emptyList(),
    val unitCount: Int? = null,
    val weightGrams: Int? = null,
    val portions: Int? = null,
    /** Freezing date, stored as UTC midnight (see util/Dates.kt). */
    val freezeDate: Long? = null,
    /** Best-before date, stored as UTC midnight. */
    val expirationDate: Long? = null,
    val notes: String = "",
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val checked: Boolean = false
)

/** Categories created on first launch (same set as the first version of the app). */
object DefaultCategories {
    val all: List<Pair<String, Long>> = listOf(
        "Légumes" to 0xFF66BB6AL,
        "Fruits" to 0xFF9CCC65L,
        "Plats complets" to 0xFF7E57C2L,
        "Viandes" to 0xFFE57373L,
        "Poissons" to 0xFFEC7FA9L,
        "Condiments" to 0xFFFFA726L,
        "Pain / Pâtisserie" to 0xFF8D6E63L,
        "Desserts / Glaces" to 0xFF26A69AL,
        "Autre" to 0xFF78909CL
    )
}
