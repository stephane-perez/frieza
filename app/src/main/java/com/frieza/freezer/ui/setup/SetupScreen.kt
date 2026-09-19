package com.frieza.freezer.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frieza.freezer.data.FriezaRepository
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    repository: FriezaRepository,
    isEditing: Boolean,
    onSaved: () -> Unit
) {
    var floorCount by remember { mutableIntStateOf(3) }
    var loaded by remember { mutableStateOf(!isEditing) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (isEditing) {
            repository.getConfigOnce()?.let { floorCount = it.floorCount }
            loaded = true
        }
    }

    if (!loaded) return

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
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

            Text(
                text = if (isEditing) "Modifier le congélateur" else "Bienvenue dans Frieza",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Combien d'étages compte ton congélateur ?",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = { if (floorCount > 1) floorCount-- }
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Moins d'étages")
                }

                Text(
                    text = floorCount.toString(),
                    style = MaterialTheme.typography.displayMedium
                )

                IconButton(
                    onClick = { if (floorCount < 20) floorCount++ }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Plus d'étages")
                }
            }

            if (isEditing) {
                Text(
                    text = "Réduire le nombre d'étages supprimera les aliments rangés sur les étages retirés.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                onClick = {
                    scope.launch {
                        repository.saveConfig(floorCount)
                        onSaved()
                    }
                }
            ) {
                Text(if (isEditing) "Enregistrer" else "Créer mon congélateur")
            }
        }
    }
}
