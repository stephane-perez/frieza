package com.frieza.freezer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.frieza.freezer.data.FriezaRepository
import com.frieza.freezer.ui.food.FoodFormScreen
import com.frieza.freezer.ui.freezer.FreezerScreen
import com.frieza.freezer.ui.setup.SetupScreen

private const val ROUTE_SETUP = "setup"
private const val ROUTE_SETUP_EDIT = "setup?editing=true"
private const val ROUTE_FREEZER = "freezer"
private const val ROUTE_FOOD_FORM = "food_form?floor={floor}&foodId={foodId}"

fun foodFormRoute(floor: Int, foodId: Long? = null): String =
    "food_form?floor=$floor&foodId=${foodId ?: -1}"

@Composable
fun FriezaNavGraph(repository: FriezaRepository) {
    val navController = rememberNavController()

    // Decide the start destination once: has the freezer already been configured?
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val config = repository.getConfigOnce()
        startDestination = if (config == null) ROUTE_SETUP else ROUTE_FREEZER
    }

    val resolvedStart = startDestination
    if (resolvedStart == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = resolvedStart) {
        composable(ROUTE_SETUP) {
            SetupScreen(
                repository = repository,
                isEditing = false,
                onSaved = {
                    navController.navigate(ROUTE_FREEZER) {
                        popUpTo(ROUTE_SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(ROUTE_SETUP_EDIT) {
            SetupScreen(
                repository = repository,
                isEditing = true,
                onSaved = { navController.popBackStack() }
            )
        }

        composable(ROUTE_FREEZER) {
            FreezerScreen(
                repository = repository,
                onEditFreezer = { navController.navigate(ROUTE_SETUP_EDIT) },
                onAddFood = { floor -> navController.navigate(foodFormRoute(floor)) },
                onEditFood = { food -> navController.navigate(foodFormRoute(food.floor, food.id)) }
            )
        }

        composable(
            route = ROUTE_FOOD_FORM,
            arguments = listOf(
                navArgument("floor") { type = NavType.IntType; defaultValue = 1 },
                navArgument("foodId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val floor = backStackEntry.arguments?.getInt("floor") ?: 1
            val foodIdArg = backStackEntry.arguments?.getLong("foodId") ?: -1L
            FoodFormScreen(
                repository = repository,
                floor = floor,
                foodId = if (foodIdArg == -1L) null else foodIdArg,
                onDone = { navController.popBackStack() }
            )
        }
    }
}
