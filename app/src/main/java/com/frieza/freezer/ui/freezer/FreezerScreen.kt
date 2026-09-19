package com.frieza.freezer.ui.freezer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.frieza.freezer.data.Food
import com.frieza.freezer.data.FriezaRepository
import com.frieza.freezer.data.QuantityType
import com.frieza.freezer.ui.theme.categoryColor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FreezerScreen(
    repository: FriezaRepository,
    onEditFreezer: () -> Unit,
    onAddFood: (floor: Int) -> Unit,
    onEditFood: (Food) -> Unit
) {
    val viewModel: FreezerViewModel = viewModel(
        factory = viewModelFactory { initializer { FreezerViewModel(repository) } }
    )

    val foods by viewModel.foods.collectAsState()
    val floorCount by viewModel.floorCount.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    // Each drawer's open/closed state. Defaults to expanded (true) so everything
    // is visible at once; the user can collapse drawers they don't need right now.
    val expandedFloors = remember { mutableStateMapOf<Int, Boolean>() }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                            val text = viewModel.buildClipboardText()
                            if (text.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(text))
                                viewModel.clearSelection()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Ingrédients copiés dans le presse-papier")
                                }
                            }
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copier pour l'IA")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Frieza") },
                    actions = {
                        IconButton(onClick = onEditFreezer) {
                            Icon(Icons.Filled.Settings, contentDescription = "Modifier le congélateur")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { onAddFood(1) }) {
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
            // Top of the list = top drawer, like a real freezer.
            for (floor in floorCount.coerceAtLeast(1) downTo 1) {
                val floorFoods = foods.filter { it.floor == floor }
                val isExpanded = expandedFloors[floor] ?: true

                DrawerSection(
                    floor = floor,
                    foods = floorFoods,
                    isExpanded = isExpanded,
                    onToggleExpanded = { expandedFloors[floor] = !isExpanded },
                    onAddFood = { onAddFood(floor) },
                    isSelectionMode = isSelectionMode,
                    selectedIds = selectedIds,
                    onFoodClick = { food ->
                        if (isSelectionMode) viewModel.toggleSelection(food.id) else onEditFood(food)
                    },
                    onFoodLongClick = { food -> viewModel.toggleSelection(food.id) }
                )
            }

            if (foods.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ton congélateur est vide.\nAppuie sur + pour ajouter un aliment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun DrawerSection(
    floor: Int,
    foods: List<Food>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAddFood: () -> Unit,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
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
                    .combinedClickable(onClick = onToggleExpanded)
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
                        Text(
                            text = "Étage $floor",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (foods.isEmpty()) "Vide" else "${foods.size} aliment(s)",
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
                            isSelectionMode = isSelectionMode,
                            isSelected = food.id in selectedIds,
                            onClick = { onFoodClick(food) },
                            onLongClick = { onFoodLongClick(food) }
                        )
                    }
                    AddChip(onClick = onAddFood)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FoodChip(
    food: Food,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val label = buildString {
        append(food.name)
        food.quantityValue?.let { value ->
            when (food.quantityType) {
                QuantityType.POIDS -> append(" - ${value}g")
                QuantityType.PORTIONS -> append(" - $value portion(s)")
                QuantityType.AUCUNE -> {}
            }
        }
    }
    val badge = expirationBadge(food.expirationDate)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = categoryColor(food.category),
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
                    tint = Color.White,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            Text(
                text = label,
                color = Color.White,
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

/** Returns a short French label when the food is expired or expiring soon, otherwise null. */
private fun expirationBadge(expirationDate: Long?): String? {
    if (expirationDate == null) return null
    val daysLeft = TimeUnit.MILLISECONDS.toDays(expirationDate - System.currentTimeMillis())
    return when {
        daysLeft < 0 -> "Périmé"
        daysLeft <= 30 -> "<1 mois"
        daysLeft <= 60 -> "<2 mois"
        else -> null
    }
}
