package com.frieza.freezer.ui.food

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
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.frieza.freezer.data.Category
import com.frieza.freezer.data.Drawer
import com.frieza.freezer.data.Food
import com.frieza.freezer.data.Freezer
import com.frieza.freezer.data.FriezaRepository
import com.frieza.freezer.util.addMonthsUtc
import com.frieza.freezer.util.formatDate
import com.frieza.freezer.util.todayUtcMillis
import kotlinx.coroutines.launch

private const val PICK_NONE = 0
private const val PICK_FREEZE = 1
private const val PICK_EXPIRATION = 2

private fun digitsOnly(input: String): String = input.filter { it.isDigit() }.take(7)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodFormScreen(
    repository: FriezaRepository,
    drawerId: Long,
    foodId: Long?,
    initialName: String,
    onDone: () -> Unit
) {
    val isEditing = foodId != null
    val scope = rememberCoroutineScope()

    val freezers by repository.freezers.collectAsState(initial = emptyList<Freezer>())
    val allDrawers by repository.drawers.collectAsState(initial = emptyList<Drawer>())
    val categories by repository.categories.collectAsState(initial = emptyList<Category>())

    var name by rememberSaveable { mutableStateOf(initialName) }
    // Selected category ids, kept as "1,2,3" so that it survives rotation.
    var categoryIdsText by rememberSaveable { mutableStateOf("") }
    var selectedDrawerId by rememberSaveable { mutableStateOf(drawerId) }
    var unitCountText by rememberSaveable { mutableStateOf("") }
    var weightText by rememberSaveable { mutableStateOf("") }
    var portionsText by rememberSaveable { mutableStateOf("") }
    var freezeMillis by rememberSaveable { mutableStateOf<Long?>(if (foodId == null) todayUtcMillis() else null) }
    var expirationMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var notes by rememberSaveable { mutableStateOf("") }
    var dateAdded by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var pickTarget by rememberSaveable { mutableStateOf(PICK_NONE) }

    val selectedCategoryIds = remember(categoryIdsText) {
        categoryIdsText.split(",").mapNotNull { it.toLongOrNull() }
    }

    LaunchedEffect(foodId) {
        if (foodId != null) {
            repository.getFood(foodId)?.let { food ->
                name = food.name
                categoryIdsText = food.categoryIds.joinToString(",")
                selectedDrawerId = food.drawerId
                unitCountText = food.unitCount?.toString() ?: ""
                weightText = food.weightGrams?.toString() ?: ""
                portionsText = food.portions?.toString() ?: ""
                freezeMillis = food.freezeDate
                expirationMillis = food.expirationDate
                notes = food.notes
                dateAdded = food.dateAdded
            }
        }
    }

    // Make sure the selected drawer exists (e.g. drawer id -1 coming from a shortcut).
    LaunchedEffect(allDrawers, freezers) {
        if (allDrawers.isNotEmpty() && allDrawers.none { it.id == selectedDrawerId }) {
            selectedDrawerId = allDrawers.first().id
        }
    }

    val currentDrawer = allDrawers.firstOrNull { it.id == selectedDrawerId }
    var selectedFreezerId by rememberSaveable { mutableStateOf(-1L) }
    val effectiveFreezerId = currentDrawer?.freezerId ?: selectedFreezerId
    val drawersOfFreezer = allDrawers.filter { it.freezerId == effectiveFreezerId }.sortedBy { it.position }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Modifier l'aliment" else "Ajouter un aliment") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (foodId != null) {
                        IconButton(onClick = {
                            scope.launch {
                                repository.duplicateFood(foodId)
                                onDone()
                            }
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Dupliquer")
                        }
                        IconButton(onClick = {
                            scope.launch {
                                repository.deleteFood(foodId)
                                onDone()
                            }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Nom de l'aliment") },
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text("Catégories", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    categories.forEach { category ->
                        val selected = category.id in selectedCategoryIds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val updated = if (selected) selectedCategoryIds - category.id
                                else selectedCategoryIds + category.id
                                categoryIdsText = updated.joinToString(",")
                            },
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
            }

            if (freezers.size > 1) {
                var freezerExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = freezerExpanded,
                    onExpandedChange = { freezerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = freezers.firstOrNull { it.id == effectiveFreezerId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Congélateur") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freezerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = freezerExpanded,
                        onDismissRequest = { freezerExpanded = false }
                    ) {
                        freezers.forEach { freezer ->
                            DropdownMenuItem(
                                text = { Text(freezer.name) },
                                onClick = {
                                    selectedFreezerId = freezer.id
                                    allDrawers.filter { it.freezerId == freezer.id }
                                        .minByOrNull { it.position }
                                        ?.let { selectedDrawerId = it.id }
                                    freezerExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            var drawerExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = drawerExpanded,
                onExpandedChange = { drawerExpanded = it }
            ) {
                OutlinedTextField(
                    value = currentDrawer?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tiroir") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = drawerExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = drawerExpanded,
                    onDismissRequest = { drawerExpanded = false }
                ) {
                    drawersOfFreezer.forEach { drawer ->
                        DropdownMenuItem(
                            text = { Text(drawer.name) },
                            onClick = {
                                selectedDrawerId = drawer.id
                                drawerExpanded = false
                            }
                        )
                    }
                }
            }

            Column {
                Text("Quantité (optionnel)", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = unitCountText,
                        onValueChange = { unitCountText = digitsOnly(it) },
                        singleLine = true,
                        label = { Text("Nombre") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = digitsOnly(it) },
                        singleLine = true,
                        label = { Text("Poids (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = portionsText,
                        onValueChange = { portionsText = digitsOnly(it) },
                        singleLine = true,
                        label = { Text("Portions") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            DateField(
                label = "Date de congélation",
                value = freezeMillis,
                onPick = { pickTarget = PICK_FREEZE },
                onClear = { freezeMillis = null }
            )

            Column {
                DateField(
                    label = "À consommer avant le",
                    value = expirationMillis,
                    onPick = { pickTarget = PICK_EXPIRATION },
                    onClear = { expirationMillis = null }
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    listOf(3, 6, 12).forEach { months ->
                        AssistChip(
                            onClick = { expirationMillis = addMonthsUtc(freezeMillis ?: todayUtcMillis(), months) },
                            label = { Text("+$months mois") }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(180) },
                label = { Text("Notes (optionnel)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && currentDrawer != null,
                onClick = {
                    val food = Food(
                        id = foodId ?: 0,
                        name = name.trim(),
                        drawerId = selectedDrawerId,
                        categoryIds = selectedCategoryIds,
                        unitCount = unitCountText.toIntOrNull(),
                        weightGrams = weightText.toIntOrNull(),
                        portions = portionsText.toIntOrNull(),
                        freezeDate = freezeMillis,
                        expirationDate = expirationMillis,
                        notes = notes.trim(),
                        dateAdded = dateAdded
                    )
                    scope.launch {
                        if (isEditing) repository.updateFood(food) else repository.addFood(food)
                        onDone()
                    }
                }
            ) {
                Text("Enregistrer")
            }
        }
    }

    if (pickTarget != PICK_NONE) {
        val initial = if (pickTarget == PICK_FREEZE) freezeMillis else expirationMillis
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initial ?: todayUtcMillis())
        DatePickerDialog(
            onDismissRequest = { pickTarget = PICK_NONE },
            confirmButton = {
                TextButton(onClick = {
                    val picked = datePickerState.selectedDateMillis
                    if (pickTarget == PICK_FREEZE) freezeMillis = picked else expirationMillis = picked
                    pickTarget = PICK_NONE
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pickTarget = PICK_NONE }) { Text("Annuler") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DateField(label: String, value: Long?, onPick: () -> Unit, onClear: () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            OutlinedButton(onClick = onPick) {
                Text(value?.let { formatDate(it) } ?: "Choisir une date")
            }
            if (value != null) {
                TextButton(onClick = onClear) { Text("Effacer") }
            }
        }
    }
}
