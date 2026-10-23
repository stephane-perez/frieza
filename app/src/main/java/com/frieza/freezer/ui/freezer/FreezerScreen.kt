package com.frieza.freezer.ui.freezer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.frieza.freezer.data.Category
import com.frieza.freezer.data.Food
import com.frieza.freezer.data.FriezaRepository
import com.frieza.freezer.util.expirationBadge
import com.frieza.freezer.util.quantityText
import com.frieza.freezer.util.todayUtcMillis
import com.frieza.freezer.util.toIsoDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val FALLBACK_COLOR = 0xFF78909CL

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FreezerScreen(
    repository: FriezaRepository,
    onAddFood: (drawerId: Long) -> Unit,
    onEditFood: (Food) -> Unit,
    onEditFreezer: (freezerId: Long) -> Unit,
    onAddFreezer: () -> Unit,
    onCategories: () -> Unit,
    onShopping: () -> Unit
) {
    val viewModel: FreezerViewModel = viewModel(
        factory = viewModelFactory { initializer { FreezerViewModel(repository) } }
    )

    val state by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }

    // Each drawer's open/closed state. Defaults to expanded so everything is visible at once.
    val expandedDrawers = remember { mutableStateMapOf<Long, Boolean>() }

    var showSearch by rememberSaveable { mutableStateOf(false) }
    var freezerMenuOpen by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    var confirmDeleteSelection by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = viewModel.exportBackup()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
                            ?: error("flux indisponible")
                    }
                    notify("Sauvegarde enregistrée")
                } catch (e: Exception) {
                    notify("Échec de l'export")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }
                } catch (e: Exception) {
                    null
                }
                if (text.isNullOrBlank()) notify("Impossible de lire ce fichier") else pendingImport = text
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} sélectionné(s)") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Annuler la sélection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.selectAll(state.drawers.flatMap { d -> d.foods.map { it.id } })
                        }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "Tout sélectionner")
                        }
                        IconButton(onClick = {
                            val text = viewModel.buildClipboardText()
                            if (text.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(text))
                                viewModel.clearSelection()
                                notify("Ingrédients copiés dans le presse-papier")
                            }
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copier pour l'IA")
                        }
                        IconButton(onClick = { confirmDeleteSelection = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer la sélection")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Box {
                            val canSwitch = state.freezers.size > 1
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(enabled = canSwitch) { freezerMenuOpen = true }
                            ) {
                                Text(state.current?.name ?: "Frieza")
                                if (canSwitch) Icon(Icons.Filled.ArrowDropDown, contentDescription = "Changer de congélateur")
                            }
                            DropdownMenu(expanded = freezerMenuOpen, onDismissRequest = { freezerMenuOpen = false }) {
                                state.freezers.forEach { freezer ->
                                    DropdownMenuItem(
                                        text = { Text(freezer.name) },
                                        leadingIcon = {
                                            if (freezer.id == state.current?.id) Icon(Icons.Filled.Check, contentDescription = null)
                                        },
                                        onClick = {
                                            viewModel.selectFreezer(freezer.id)
                                            freezerMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            showSearch = !showSearch
                            if (!showSearch) viewModel.clearFilters()
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = "Rechercher")
                        }
                        Box {
                            IconButton(onClick = { overflowOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                                SortMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text("Trier : ${mode.label}") },
                                        leadingIcon = {
                                            if (sort == mode) Icon(Icons.Filled.Check, contentDescription = null)
                                        },
                                        onClick = {
                                            viewModel.setSort(mode)
                                            overflowOpen = false
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Liste de courses") },
                                    onClick = { overflowOpen = false; onShopping() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Catégories") },
                                    onClick = { overflowOpen = false; onCategories() }
                                )
                                HorizontalDivider()
                                state.current?.let { current ->
                                    DropdownMenuItem(
                                        text = { Text("Modifier « ${current.name} »") },
                                        onClick = { overflowOpen = false; onEditFreezer(current.id) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Ajouter un congélateur") },
                                    onClick = { overflowOpen = false; onAddFreezer() }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Exporter une sauvegarde") },
                                    onClick = {
                                        overflowOpen = false
                                        exportLauncher.launch("frieza-${toIsoDate(todayUtcMillis())}.json")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Importer (Frieza ou Frizor)") },
                                    onClick = {
                                        overflowOpen = false
                                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode && state.drawers.isNotEmpty()) {
                FloatingActionButton(onClick = { onAddFood(state.drawers.first().drawer.id) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Ajouter un aliment")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            if (showSearch && !isSelectionMode) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.setQuery(it) },
                    singleLine = true,
                    label = { Text("Rechercher un aliment") },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Effacer")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    state.categories.forEach { category ->
                        FilterChip(
                            selected = category.id in categoryFilter,
                            onClick = { viewModel.toggleCategoryFilter(category.id) },
                            label = { Text(category.name) },
                            leadingIcon = {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .background(Color(category.color), CircleShape)
                                )
                            }
                        )
                    }
                }
                if (state.filterActive) {
                    Text(
                        text = "${state.matchCount} résultat(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
            }

            state.drawers.forEach { drawerUi ->
                // While filtering, drawers without a match are hidden.
                if (state.filterActive && drawerUi.foods.isEmpty()) return@forEach
                val drawerId = drawerUi.drawer.id
                val isExpanded = expandedDrawers[drawerId] ?: true

                DrawerSection(
                    name = drawerUi.drawer.name,
                    foods = drawerUi.foods,
                    totalCount = drawerUi.totalCount,
                    filterActive = state.filterActive,
                    categoriesById = categoriesById,
                    isExpanded = isExpanded,
                    onToggleExpanded = { expandedDrawers[drawerId] = !isExpanded },
                    onAddFood = { onAddFood(drawerId) },
                    selectedIds = selectedIds,
                    isSelectionMode = isSelectionMode,
                    onFoodClick = { food ->
                        if (isSelectionMode) viewModel.toggleSelection(food.id) else onEditFood(food)
                    },
                    onFoodLongClick = { food -> viewModel.toggleSelection(food.id) }
                )
            }

            if (state.current != null && state.drawers.isEmpty()) {
                EmptyMessage("Ce congélateur n'a pas encore de tiroir.\nOuvre le menu ⋮ puis « Modifier » pour en ajouter.")
            } else if (state.drawers.isNotEmpty() && state.drawers.all { it.totalCount == 0 }) {
                EmptyMessage("Ton congélateur est vide.\nAppuie sur + pour ajouter un aliment.")
            } else if (state.filterActive && state.matchCount == 0) {
                EmptyMessage("Aucun aliment ne correspond à la recherche.")
            }
        }
    }

    if (confirmDeleteSelection) {
        AlertDialog(
            onDismissRequest = { confirmDeleteSelection = false },
            title = { Text("Supprimer ${selectedIds.size} aliment(s) ?") },
            text = { Text("Cette action est définitive.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteSelection = false
                    viewModel.deleteSelected()
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSelection = false }) { Text("Annuler") }
            }
        )
    }

    pendingImport?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Importer cette sauvegarde ?") },
            text = {
                Text(
                    "Toutes les données actuelles (congélateurs, aliments, catégories, liste de courses) " +
                        "seront remplacées. Pense à exporter d'abord si tu veux les conserver."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        try {
                            val summary = viewModel.importBackup(text)
                            notify("Importé : $summary")
                        } catch (e: IllegalArgumentException) {
                            notify(e.message ?: "Sauvegarde invalide")
                        } catch (e: Exception) {
                            notify("Échec de l'import")
                        }
                    }
                }) { Text("Remplacer") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun DrawerSection(
    name: String,
    foods: List<Food>,
    totalCount: Int,
    filterActive: Boolean,
    categoriesById: Map<Long, Category>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAddFood: () -> Unit,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onFoodClick: (Food) -> Unit,
    onFoodLongClick: (Food) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Drawer front: tapping anywhere on it opens/closes the drawer.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Kitchen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(text = name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when {
                                totalCount == 0 -> "Vide"
                                filterActive -> "${foods.size} / $totalCount aliment(s)"
                                else -> "$totalCount aliment(s)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Replier" else "Déplier"
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    foods.forEach { food ->
                        FoodChip(
                            food = food,
                            categoriesById = categoriesById,
                            isSelected = food.id in selectedIds,
                            onClick = { onFoodClick(food) },
                            onLongClick = { onFoodLongClick(food) }
                        )
                    }
                    if (!isSelectionMode) AddChip(onClick = onAddFood)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FoodChip(
    food: Food,
    categoriesById: Map<Long, Category>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val categories = food.categoryIds.mapNotNull { categoriesById[it] }
    val background = Color(categories.firstOrNull()?.color ?: FALLBACK_COLOR)
    val foreground = if (background.luminance() > 0.6f) Color.Black else Color.White
    val quantity = quantityText(food)
    val label = if (quantity.isNotBlank()) "${food.name} - $quantity" else food.name
    val badge = expirationBadge(food.expirationDate)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = background,
        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            // Extra categories appear as small dots in their own colors.
            categories.drop(1).forEach { extra ->
                Box(
                    Modifier
                        .padding(end = 4.dp)
                        .size(10.dp)
                        .background(Color(extra.color), CircleShape)
                )
            }
            Text(
                text = label,
                color = foreground,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            badge?.let {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = it,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddChip(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text("Ajouter", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
