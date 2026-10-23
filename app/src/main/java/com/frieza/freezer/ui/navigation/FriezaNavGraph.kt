package com.frieza.freezer.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.frieza.freezer.data.FriezaRepository
import com.frieza.freezer.ui.category.CategoriesScreen
import com.frieza.freezer.ui.food.FoodFormScreen
import com.frieza.freezer.ui.freezer.FreezerScreen
import com.frieza.freezer.ui.setup.FreezerEditScreen
import com.frieza.freezer.ui.setup.SetupScreen
import com.frieza.freezer.ui.shopping.ShoppingScreen
import kotlinx.coroutines.launch

private const val ROUTE_SETUP_FIRST = "setup_first"
private const val ROUTE_SETUP_NEW = "setup_new"
private const val ROUTE_FREEZER = "freezer"
private const val ROUTE_FREEZER_EDIT = "freezer_edit/{freezerId}"
private const val ROUTE_CATEGORIES = "categories"
private const val ROUTE_SHOPPING = "shopping"
private const val ROUTE_FOOD_FORM = "food_form?drawerId={drawerId}&foodId={foodId}&name={name}"

fun foodFormRoute(drawerId: Long, foodId: Long? = null, name: String = ""): String =
    "food_form?drawerId=$drawerId&foodId=${foodId ?: -1}&name=${Uri.encode(name)}"

private fun NavController.resetTo(route: String) {
    navigate(route) { popUpTo(graph.id) { inclusive = true } }
}

@Composable
fun FriezaNavGraph(repository: FriezaRepository) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Decide the start destination once: has a freezer already been created?
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        startDestination = if (repository.hasFreezer()) ROUTE_FREEZER else ROUTE_SETUP_FIRST
    }

    val resolvedStart = startDestination
    if (resolvedStart == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = resolvedStart) {
        composable(ROUTE_SETUP_FIRST) {
            SetupScreen(
                repository = repository,
                isFirstRun = true,
                onSaved = { navController.resetTo(ROUTE_FREEZER) },
                onBack = {}
            )
        }

        composable(ROUTE_SETUP_NEW) {
            SetupScreen(
                repository = repository,
                isFirstRun = false,
                onSaved = { navController.resetTo(ROUTE_FREEZER) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_FREEZER) {
            FreezerScreen(
                repository = repository,
                onAddFood = { drawerId -> navController.navigate(foodFormRoute(drawerId)) },
                onEditFood = { food -> navController.navigate(foodFormRoute(food.drawerId, food.id)) },
                onEditFreezer = { id -> navController.navigate("freezer_edit/$id") },
                onAddFreezer = { navController.navigate(ROUTE_SETUP_NEW) },
                onCategories = { navController.navigate(ROUTE_CATEGORIES) },
                onShopping = { navController.navigate(ROUTE_SHOPPING) }
            )
        }

        composable(
            route = ROUTE_FREEZER_EDIT,
            arguments = listOf(navArgument("freezerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val freezerId = backStackEntry.arguments?.getLong("freezerId") ?: -1L
            FreezerEditScreen(
                repository = repository,
                freezerId = freezerId,
                onBack = { navController.popBackStack() },
                onFreezerDeleted = {
                    scope.launch {
                        navController.resetTo(if (repository.hasFreezer()) ROUTE_FREEZER else ROUTE_SETUP_FIRST)
                    }
                }
            )
        }

        composable(ROUTE_CATEGORIES) {
            CategoriesScreen(repository = repository, onBack = { navController.popBackStack() })
        }

        composable(ROUTE_SHOPPING) {
            ShoppingScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onStoreInFreezer = { name ->
                    scope.launch {
                        navController.navigate(foodFormRoute(repository.defaultDrawerId(), null, name))
                    }
                }
            )
        }

        composable(
            route = ROUTE_FOOD_FORM,
            arguments = listOf(
                navArgument("drawerId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("foodId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val drawerId = backStackEntry.arguments?.getLong("drawerId") ?: -1L
            val foodIdArg = backStackEntry.arguments?.getLong("foodId") ?: -1L
            val name = backStackEntry.arguments?.getString("name") ?: ""
            FoodFormScreen(
                repository = repository,
                drawerId = drawerId,
                foodId = if (foodIdArg == -1L) null else foodIdArg,
                initialName = name,
                onDone = { navController.popBackStack() }
            )
        }
    }
}
