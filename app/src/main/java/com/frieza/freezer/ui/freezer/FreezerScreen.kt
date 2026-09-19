package com.frieza.freezer.ui.freezer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.frieza.freezer.data.Food
import com.frieza.freezer.data.FriezaRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
        if (foods.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ton congélateur est vide.\nAppuie sur + pour ajouter un aliment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                for (floor in 1..floorCount.coerceAtLeast(1)) {
                    val floorFoods = foods.filter { it.floor == floor }

                    stickyHeader(key = "header-$floor") {
                        FloorHeader(floor = floor, count = floorFoods.size, onAddFood = { onAddFood(floor) })
                    }

                    if (floorFoods.isEmpty()) {
                        item(key = "empty-$floor") {
                            Text(
                                text = "Aucun aliment sur cet étage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        items(floorFoods, key = { it.id }) { food ->
                            FoodRow(
                                food = food,
                                isSelectionMode = isSelectionMode,
                                isSelected = food.id in selectedIds,
                                onClick = {
                                    if (isSelectionMode) viewModel.toggleSelection(food.id) else onEditFood(food)
                                },
                                onLongClick = { viewModel.toggleSelection(food.id) },
                                onDelete = { viewModel.deleteFood(food) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloorHeader(floor: Int, count: Int, onAddFood: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Étage $floor" + if (count > 0) " · $count" else "",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onAddFood) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un aliment à cet étage")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FoodRow(
    food: Food,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Column {
                    Text(text = food.name, style = MaterialTheme.typography.titleMedium)
                    val quantity = food.quantityValue?.let {
                        when (food.quantityType) {
                            com.frieza.freezer.data.QuantityType.POIDS -> "$it g"
                            com.frieza.freezer.data.QuantityType.PORTIONS -> "$it portion(s)"
                            com.frieza.freezer.data.QuantityType.AUCUNE -> null
                        }
                    }
                    val subtitle = listOfNotNull(
                        food.category.label,
                        quantity,
                        food.expirationDate?.let { "avant le ${dateFormat.format(Date(it))}" }
                    ).joinToString(" · ")
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isSelectionMode) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                }
            }
        }
    }
}