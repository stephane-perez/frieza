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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frieza.freezer.data.FriezaRepository
import kotlinx.coroutines.launch

/** Creates a freezer (first launch, or "Ajouter un congélateur"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    repository: FriezaRepository,
    isFirstRun: Boolean,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(if (isFirstRun) "Congélateur" else "") }
    var drawerCount by rememberSaveable { mutableIntStateOf(3) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            if (!isFirstRun) {
                TopAppBar(
                    title = { Text("Nouveau congélateur") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AcUnit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isFirstRun) {
                Text(
                    text = "Bienvenue dans Frieza",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Nom du congélateur") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            Text(
                text = "Combien de tiroirs / étages ?",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = { if (drawerCount > 1) drawerCount-- }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Moins de tiroirs")
                }
                Text(text = drawerCount.toString(), style = MaterialTheme.typography.displayMedium)
                IconButton(onClick = { if (drawerCount < 20) drawerCount++ }) {
                    Icon(Icons.Filled.Add, contentDescription = "Plus de tiroirs")
                }
            }

            Text(
                text = "Tu pourras renommer, réordonner, ajouter ou supprimer des tiroirs ensuite.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                enabled = name.isNotBlank(),
                onClick = {
                    scope.launch {
                        repository.createFreezer(name, drawerCount)
                        onSaved()
                    }
                }
            ) {
                Text(if (isFirstRun) "Créer mon congélateur" else "Créer")
            }
        }
    }
}
