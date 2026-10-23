package com.frieza.freezer.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.frieza.freezer.data.Category
import com.frieza.freezer.data.FriezaRepository
import kotlinx.coroutines.launch

val CategoryPalette: List<Long> = listOf(
    0xFF66BB6AL, 0xFF9CCC65L, 0xFFD4E157L, 0xFFFFCA28L, 0xFFFFA726L, 0xFFFF7043L,
    0xFFE57373L, 0xFFEC7FA9L, 0xFFBA68C8L, 0xFF7E57C2L, 0xFF5C6BC0L, 0xFF42A5F5L,
    0xFF26C6DAL, 0xFF26A69AL, 0xFF8D6E63L, 0xFF78909CL
)

private sealed interface CategoryDialog {
    data object Create : CategoryDialog
    data class Edit(val category: Category) : CategoryDialog
    data class Delete(val category: Category) : CategoryDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(repository: FriezaRepository, onBack: () -> Unit) {
    val categories by repository.categories.collectAsState(initial = emptyList<Category>())
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<CategoryDialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catégories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialog = CategoryDialog.Create }) {
                Icon(Icons.Filled.Add, contentDescription = "Nouvelle catégorie")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .background(Color(category.color), CircleShape)
                        )
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        )
                        IconButton(onClick = { dialog = CategoryDialog.Edit(category) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Modifier")
                        }
                        IconButton(onClick = { dialog = CategoryDialog.Delete(category) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                        }
                    }
                }
            }
            item {
                Text(
                    "Un aliment peut avoir plusieurs catégories ; la première donne sa couleur.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }

    when (val d = dialog) {
        null -> Unit
        is CategoryDialog.Create -> CategoryEditorDialog(
            title = "Nouvelle catégorie",
            initialName = "",
            initialColor = CategoryPalette[11],
            onDismiss = { dialog = null },
            onConfirm = { name, color ->
                dialog = null
                scope.launch { repository.addCategory(name, color) }
            }
        )
        is CategoryDialog.Edit -> CategoryEditorDialog(
            title = "Modifier la catégorie",
            initialName = d.category.name,
            initialColor = d.category.color,
            onDismiss = { dialog = null },
            onConfirm = { name, color ->
                dialog = null
                scope.launch { repository.updateCategory(d.category.copy(name = name.trim(), color = color)) }
            }
        )
        is CategoryDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Supprimer « ${d.category.name} » ?") },
            text = { Text("Elle sera retirée des aliments qui l'utilisent (les aliments ne sont pas supprimés).") },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    scope.launch { repository.deleteCategory(d.category.id) }
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Annuler") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    title: String,
    initialName: String,
    initialColor: Long,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }
    val palette = remember(initialColor) {
        if (initialColor in CategoryPalette) CategoryPalette else CategoryPalette + initialColor
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    palette.forEach { option ->
                        val selected = option == color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(option), CircleShape)
                                .clickable { color = option },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = "Choisie", tint = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name, color) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
