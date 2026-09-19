package com.frieza.freezer.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.frieza.freezer.data.Food
import com.frieza.freezer.data.FoodCategory
import com.frieza.freezer.data.FriezaRepository
import com.frieza.freezer.data.QuantityType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodFormScreen(
    repository: FriezaRepository,
    floor: Int,
    foodId: Long?,
    onDone: () -> Unit
) {
    val isEditing = foodId != null
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(FoodCategory.AUTRE) }
    var selectedFloor by rememberSaveable { mutableIntStateOf(floor) }
    var floorCount by rememberSaveable { mutableIntStateOf(floor) }
    var quantityType by rememberSaveable { mutableStateOf(QuantityType.AUCUNE) }
    var quantityValueText by rememberSaveable { mutableStateOf("") }
    var expirationMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var dateAdded by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        repository.getConfigOnce()?.let { floorCount = it.floorCount }
        if (isEditing && foodId != null) {
            repository.getFood(foodId)?.let { food ->
                name = food.name
                category = food.category
                selectedFloor = food.floor
                quantityType = food.quantityType
                quantityValueText = food.quantityValue?.toString() ?: ""
                expirationMillis = food.expirationDate
                dateAdded = food.dateAdded
            }
        }
    }

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
                    if (isEditing) {
                        IconButton(onClick = {
                            scope.launch {
                                repository.getFood(foodId!!)?.let { repository.deleteFood(it) }
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
                label = { Text("Nom de l'aliment") },
                modifier = Modifier.fillMaxWidth()
            )

            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = category.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Catégorie") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                androidx.compose.material3.ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    FoodCategory.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                category = option
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            if (floorCount > 1) {
                var floorExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = floorExpanded,
                    onExpandedChange = { floorExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "Étage $selectedFloor",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Étage") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = floorExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    androidx.compose.material3.ExposedDropdownMenu(
                        expanded = floorExpanded,
                        onDismissRequest = { floorExpanded = false }
                    ) {
                        (1..floorCount).forEach { f ->
                            DropdownMenuItem(
                                text = { Text("Étage $f") },
                                onClick = {
                                    selectedFloor = f
                                    floorExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Column {
                Text("Quantité", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    QuantityType.entries.forEach { type ->
                        FilterChip(
                            selected = quantityType == type,
                            onClick = {
                                quantityType = type
                                if (type == QuantityType.AUCUNE) quantityValueText = ""
                            },
                            label = { Text(type.label) }
                        )
                    }
                }
            }

            if (quantityType != QuantityType.AUCUNE) {
                OutlinedTextField(
                    value = quantityValueText,
                    onValueChange = { input -> if (input.all { it.isDigit() }) quantityValueText = input },
                    label = { Text(if (quantityType == QuantityType.POIDS) "Poids (g)" else "Nombre de portions") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column {
                Text("Date de péremption (optionnel)", style = MaterialTheme.typography.labelLarge)
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text(expirationMillis?.let { dateFormat.format(Date(it)) } ?: "Choisir une date")
                    }
                    if (expirationMillis != null) {
                        TextButton(onClick = { expirationMillis = null }) {
                            Text("Effacer")
                        }
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
                onClick = {
                    val quantityValue = quantityValueText.toIntOrNull()
                    val food = Food(
                        id = foodId ?: 0,
                        name = name.trim(),
                        category = category,
                        floor = selectedFloor,
                        quantityType = quantityType,
                        quantityValue = if (quantityType == QuantityType.AUCUNE) null else quantityValue,
                        expirationDate = expirationMillis,
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = expirationMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    expirationMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annuler") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
