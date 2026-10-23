package com.frieza.freezer.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.frieza.freezer.data.FriezaRepository
import com.frieza.freezer.data.ShoppingItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    repository: FriezaRepository,
    onBack: () -> Unit,
    onStoreInFreezer: (name: String) -> Unit
) {
    val items by repository.shopping.collectAsState(initial = emptyList<ShoppingItem>())
    val scope = rememberCoroutineScope()
    var newItem by remember { mutableStateOf("") }

    fun submit() {
        if (newItem.isBlank()) return
        val text = newItem
        newItem = ""
        scope.launch { repository.addShoppingItem(text) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Liste de courses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (items.any { it.checked }) {
                        IconButton(onClick = { scope.launch { repository.clearCheckedShopping() } }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Retirer les articles cochés")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newItem,
                        onValueChange = { newItem = it },
                        singleLine = true,
                        label = { Text("Ajouter un article") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { submit() }, enabled = newItem.isNotBlank()) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter")
                    }
                }
            }
            items(items, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.checked,
                            onCheckedChange = { scope.launch { repository.setShoppingChecked(item, it) } }
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            scope.launch { repository.setShoppingChecked(item, true) }
                            onStoreInFreezer(item.name)
                        }) {
                            Icon(Icons.Filled.Kitchen, contentDescription = "Ranger au congélateur")
                        }
                        IconButton(onClick = { scope.launch { repository.deleteShoppingItem(item.id) } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Retirer")
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        "Rien à acheter pour le moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
