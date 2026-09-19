package com.frieza.freezer.ui.freezer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frieza.freezer.data.Food
import com.frieza.freezer.data.FriezaRepository
import com.frieza.freezer.data.QuantityType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FreezerViewModel(private val repository: FriezaRepository) : ViewModel() {

    val foods: StateFlow<List<Food>> = repository.foods.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val floorCount: StateFlow<Int> = repository.config
        .map { it?.floorCount ?: 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    val isSelectionMode: StateFlow<Boolean> = _selectedIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.let { current ->
            if (id in current) current - id else current + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteFood(food: Food) {
        viewModelScope.launch { repository.deleteFood(food) }
    }

    /** Builds a ready-to-paste prompt for an AI assistant from the selected foods. */
    fun buildClipboardText(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        val selected = foods.value.filter { it.id in _selectedIds.value }

        if (selected.isEmpty()) return ""

        val lines = selected.joinToString("\n") { food ->
            val quantity = when (food.quantityType) {
                QuantityType.POIDS -> food.quantityValue?.let { "${it} g" } ?: ""
                QuantityType.PORTIONS -> food.quantityValue?.let { "${it} portion(s)" } ?: ""
                QuantityType.AUCUNE -> ""
            }
            val expiration = food.expirationDate?.let {
                " – à consommer avant le ${dateFormat.format(Date(it))}"
            } ?: ""
            val details = listOfNotNull(
                food.category.label,
                quantity.takeIf { it.isNotBlank() }
            ).joinToString(", ")
            "- ${food.name} ($details)$expiration"
        }

        return "Voici les ingrédients dont je dispose au congélateur :\n$lines\n\n" +
            "Propose-moi 3 idées de recettes réalisables avec ces ingrédients " +
            "(je peux compléter avec des produits de base comme sel, huile, épices)."
    }
}
