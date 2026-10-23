package com.frieza.freezer.util

import com.frieza.freezer.data.Food

/** "2×, 1,5 kg, 4 portions" – only the parts that are filled in. */
fun quantityText(food: Food): String {
    val parts = mutableListOf<String>()
    food.unitCount?.let { parts += "$it×" }
    food.weightGrams?.let { parts += formatWeight(it) }
    food.portions?.let { parts += if (it > 1) "$it portions" else "$it portion" }
    return parts.joinToString(", ")
}

/** Short badge for foods that are expired or about to expire, null otherwise. */
fun expirationBadge(expirationDate: Long?): String? {
    if (expirationDate == null) return null
    val days = daysUntil(expirationDate)
    return when {
        days < 0 -> "Périmé"
        days <= 30 -> "<1 mois"
        days <= 60 -> "<2 mois"
        else -> null
    }
}
