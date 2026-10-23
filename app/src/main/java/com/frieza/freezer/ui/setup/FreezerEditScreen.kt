package com.frieza.freezer.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frieza.freezer.data.Drawer
import com.frieza.freezer.data.Freezer
import com.frieza.freezer.data.FriezaRepository
import kotlinx.coroutines.launch

private sealed interface EditDialog {
    data object AddDrawer : EditDialog
    data class RenameDrawer(val drawer: Drawer) : EditDialog
    data class DeleteDrawer(val drawer: Drawer, val foodCount: Int) : EditDialog
    data class DeleteFreezer(val foodCount: Int) : EditDialog
}

/** Rename a freezer, manage its drawers (rename / reorder / add / delete) or delete it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreezerEditScreen(
    repository: FriezaRepository,
    freezerId: Long,
    onBack: () -> Unit,
    onFreezerDeleted: () -> Unit
) {
    val freezers by repository.freezers.collectAsState(initial = emptyList<Freezer>())
    val allDrawers by repository.drawers.collectAsState(initial = emptyList<Drawer>())
    val freezer = freezers.firstOrNull { it.id == freezerId }
    val drawers = remember(allDrawers, freezerId) {
        allDrawers.filter { it.freezerId == freezerId }.sortedBy { it.position }
    }
    val scope = rememberCoroutineScope()

    var nameText by remember { mutableStateOf("") }
    var nameLoaded by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<EditDialog?>(null) }

    LaunchedEffect(freezer?.id) {
        if (freezer != null && !nameLoaded) {
            nameText = freezer.name
            nameLoaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier le congélateur") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        if (freezer == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                singleLine = true,
                label = { Text("Nom du congélateur") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = nameText.isNotBlank() && nameText.trim() != freezer.name,
                onClick = { scope.launch { repository.renameFreezer(freezer, nameText) } }
            ) { Text("Renommer") }

            Text(
                "Tiroirs (du haut vers le bas)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            drawers.forEachIndexed { index, drawer ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = drawer.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            enabled = index > 0,
                            onClick = { scope.launch { repository.moveDrawer(freezerId, drawer.id, up = true) } }
                        ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Monter") }
                        IconButton(
                            enabled = index < drawers.lastIndex,
                            onClick = { scope.launch { repository.moveDrawer(freezerId, drawer.id, up = false) } }
                        ) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Descendre") }
                        IconButton(onClick = { dialog = EditDialog.RenameDrawer(drawer) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Renommer")
                        }
                        IconButton(onClick = {
                            scope.launch {
                                dialog = EditDialog.DeleteDrawer(drawer, repository.foodCountInDrawer(drawer.id))
                            }
                        }) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer") }
                    }
                }
            }

            OutlinedButton(onClick = { dialog = EditDialog.AddDrawer }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Ajouter un tiroir")
            }

            Button(
                onClick = {
                    scope.launch { dialog = EditDialog.DeleteFreezer(repository.foodCountInFreezer(freezerId)) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) { Text("Supprimer ce congélateur") }
        }
    }

    when (val d = dialog) {
        null -> Unit
        is EditDialog.AddDrawer -> NameDialog(
            title = "Nouveau tiroir",
            initial = "Tiroir ${drawers.size + 1}",
            onDismiss = { dialog = null },
            onConfirm = { text ->
                dialog = null
                scope.launch { repository.addDrawer(freezerId, text) }
            }
        )
        is EditDialog.RenameDrawer -> NameDialog(
            title = "Renommer le tiroir",
            initial = d.drawer.name,
            onDismiss = { dialog = null },
            onConfirm = { text ->
                dialog = null
                scope.launch { repository.renameDrawer(d.drawer, text) }
            }
        )
        is EditDialog.DeleteDrawer -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Supprimer « ${d.drawer.name} » ?") },
            text = {
                Text(
                    if (d.foodCount > 0) "Les ${d.foodCount} aliment(s) qu'il contient seront aussi supprimés."
                    else "Ce tiroir est vide."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    scope.launch { repository.deleteDrawer(d.drawer.id) }
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Annuler") } }
        )
        is EditDialog.DeleteFreezer -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Supprimer « ${freezer?.name ?: ""} » ?") },
            text = {
                Text(
                    if (d.foodCount > 0) "Ses tiroirs et les ${d.foodCount} aliment(s) qu'ils contiennent seront supprimés."
                    else "Ses tiroirs seront supprimés."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    scope.launch {
                        repository.deleteFreezer(freezerId)
                        onFreezerDeleted()
                    }
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Annuler") } }
        )
    }
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
