package com.frieza.freezer.ui.theme

import androidx.compose.ui.graphics.Color
import com.frieza.freezer.data.FoodCategory

/** One saturated color per category, used for the food chips on the freezer screen. */
fun categoryColor(category: FoodCategory): Color = when (category) {
    FoodCategory.LEGUMES -> Color(0xFF66BB6A)
    FoodCategory.FRUITS -> Color(0xFF9CCC65)
    FoodCategory.PLATS_COMPLETS -> Color(0xFF7E57C2)
    FoodCategory.VIANDES -> Color(0xFFE57373)
    FoodCategory.POISSONS -> Color(0xFFEC7FA9)
    FoodCategory.CONDIMENTS -> Color(0xFFFFA726)
    FoodCategory.PAIN_PATISSERIE -> Color(0xFF8D6E63)
    FoodCategory.DESSERTS_GLACES -> Color(0xFF26A69A)
    FoodCategory.AUTRE -> Color(0xFF78909C)
}
