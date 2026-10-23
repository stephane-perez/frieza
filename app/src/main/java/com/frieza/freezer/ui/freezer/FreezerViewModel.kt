package com.frieza.freezer.ui.freezer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frieza.freezer.data.Category
import com.frieza.freezer.data.Drawer
import com.frieza.freezer.data.Food
import com.frieza.freezer.data.Freezer
import com.frieza.freezer.data.FriezaRepository
import com.frieza.freezer.util.formatDate
import com.frieza.freezer.util.quantityText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortMode(val label: String) {
    NAME("Nom"),
    EXPIRATION("Péremption"),
    RECENT("Ajout récent")
}

data class DrawerUi(val drawer: Drawer, val foods: List<Food>, val totalCount: Int)

data class FreezerUiState(
    val freezers: List<Freezer> = emptyList(),
    val current: Freezer? = null,
    val drawers: List<DrawerUi> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filterActive: Boolean = false,
    val matchCount: Int = 0
)

private data class Base(
    val freezers: List<Freezer>,
    val drawers: List<Drawer>,
    val categories: List<Category>,
    val foods: List<Food>,
    val selectedId: Long
)

private data class Filters(val query: String, val categories: Set<Long>, val sort: SortMode)

class FreezerViewModel(private val repository: FriezaRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _categoryFilter = MutableStateFlow<Set<Long>>(emptySet())
    val categoryFilter: StateFlow<Set<Long>> = _categoryFilter

    private val _sort = MutableStateFlow(SortMode.NAME)
    val sort: StateFlow<SortMode> = _sort

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    val isSelectionMode: StateFlow<Boolean> = _selectedIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val base = combine(
        repository.freezers,
        repository.drawers,
        repository.categories,
        repository.foods,
        repository.selectedFreezerId
    ) { freezers, drawers, categories, foods, selected ->
        Base(freezers, drawers, categories, foods, selected)
    }

    private val filters = combine(_query, _categoryFilter, _sort) { q, c, s -> Filters(q, c, s) }

    val uiState: StateFlow<FreezerUiState> = combine(base, filters) { b, f ->
        val current = b.freezers.firstOrNull { it.id == b.selectedId } ?: b.freezers.firstOrNull()
        val query = f.query.trim().lowercase()
        val filterActive = query.isNotEmpty() || f.categories.isNotEmpty()
        val comparator: Comparator<Food> = when (f.sort) {
            SortMode.NAME -> compareBy { it.name.lowercase() }
            SortMode.EXPIRATION -> compareBy<Food> { it.expirationDate ?: Long.MAX_VALUE }.thenBy { it.name.lowercase() }
            SortMode.RECENT -> compareByDescending { it.dateAdded }
        }
        var matches = 0
        val drawers = b.drawers
            .filter { it.freezerId == current?.id }
            .sortedBy { it.position }
            .map { drawer ->
                val inDrawer = b.foods.filter { it.drawerId == drawer.id }
                val visible = inDrawer
                    .filter { food ->
                        (query.isEmpty() || food.name.lowercase().contains(query) || food.notes.lowercase().contains(query)) &&
                            (f.categories.isEmpty() || food.categoryIds.any { it in f.categories })
                    }
                    .sortedWith(comparator)
                matches += visible.size
                DrawerUi(drawer, visible, inDrawer.size)
            }
        FreezerUiState(b.freezers, current, drawers, b.categories, filterActive, matches)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FreezerUiState())

    private val allFoods: StateFlow<List<Food>> = repository.foods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val allCategories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- filters ----

    fun setQuery(value: String) { _query.value = value }
    fun setSort(mode: SortMode) { _sort.value = mode }
    fun toggleCategoryFilter(id: Long) {
        _categoryFilter.value = _categoryFilter.value.let { if (id in it) it - id else it + id }
    }
    fun clearFilters() {
        _query.value = ""
        _categoryFilter.value = emptySet()
    }

    // ---- freezers ----

    fun selectFreezer(id: Long) {
        clearSelection()
        repository.selectFreezer(id)
    }

    // ---- selection ----

    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.let { current ->
            if (id in current) current - id else current + id
        }
    }

    fun selectAll(ids: Collection<Long>) {
        _selectedIds.value = _selectedIds.value + ids
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        val ids = _selectedIds.value
        viewModelScope.launch {
            ids.forEach { repository.deleteFood(it) }
            clearSelection()
        }
    }

    // ---- backup ----

    suspend fun exportBackup(): String = repository.exportBackup()

    suspend fun importBackup(text: String): String {
        clearFilters()
        clearSelection()
        return repository.importBackup(text)
    }

    /** Builds a ready-to-paste prompt for an AI assistant from the selected foods. */
    fun buildClipboardText(): String {
        val categoriesById = allCategories.value.associateBy { it.id }
        val selected = allFoods.value.filter { it.id in _selectedIds.value }
        if (selected.isEmpty()) return ""

        val lines = selected.joinToString("\n") { food ->
            val details = listOfNotNull(
                food.categoryIds.mapNotNull { categoriesById[it]?.name }.joinToString(" / ").takeIf { it.isNotBlank() },
                quantityText(food).takeIf { it.isNotBlank() }
            ).joinToString(", ")
            val detailsText = if (details.isNotBlank()) " ($details)" else ""
            val expiration = food.expirationDate?.let { " – à consommer avant le ${formatDate(it)}" } ?: ""
            "- ${food.name}$detailsText$expiration"
        }

        return "Voici les ingrédients dont je dispose au congélateur :\n$lines\n\n" +
            "Propose-moi 3 idées de recettes réalisables avec ces ingrédients " +
            "(je peux compléter avec des produits de base comme sel, huile, épices)."
    }
}
