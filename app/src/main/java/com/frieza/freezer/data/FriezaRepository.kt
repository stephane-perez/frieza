package com.frieza.freezer.data

import android.content.SharedPreferences
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FriezaRepository(
    private val db: FriezaDatabase,
    private val prefs: SharedPreferences
) {
    private val freezerDao = db.freezerDao()
    private val drawerDao = db.drawerDao()
    private val categoryDao = db.categoryDao()
    private val foodDao = db.foodDao()
    private val shoppingDao = db.shoppingDao()

    val freezers: Flow<List<Freezer>> = freezerDao.observeAll()
    val drawers: Flow<List<Drawer>> = drawerDao.observeAll()
    val categories: Flow<List<Category>> = categoryDao.observeAll()
    val foods: Flow<List<Food>> = foodDao.observeAll()
    val shopping: Flow<List<ShoppingItem>> = shoppingDao.observeAll()

    // ---- Which freezer is displayed (remembered between launches) ----

    private val _selectedFreezerId = MutableStateFlow(prefs.getLong(KEY_SELECTED_FREEZER, -1L))
    val selectedFreezerId: StateFlow<Long> = _selectedFreezerId

    fun selectFreezer(id: Long) {
        _selectedFreezerId.value = id
        prefs.edit().putLong(KEY_SELECTED_FREEZER, id).apply()
    }

    suspend fun hasFreezer(): Boolean = freezerDao.getAll().isNotEmpty()

    /** Top drawer of the displayed freezer, or -1 when there is none. */
    suspend fun defaultDrawerId(): Long {
        val all = freezerDao.getAll()
        val freezer = all.firstOrNull { it.id == _selectedFreezerId.value } ?: all.firstOrNull() ?: return -1L
        return drawerDao.getByFreezer(freezer.id).firstOrNull()?.id ?: -1L
    }

    // ---- Freezers & drawers ----

    suspend fun createFreezer(name: String, drawerCount: Int): Long {
        val id = db.withTransaction {
            val position = freezerDao.getAll().size
            val freezerId = freezerDao.insert(Freezer(name = name.trim(), position = position))
            for (i in drawerCount downTo 1) {
                drawerDao.insert(
                    Drawer(freezerId = freezerId, name = "Étage $i", position = drawerCount - i)
                )
            }
            freezerId
        }
        selectFreezer(id)
        return id
    }

    suspend fun renameFreezer(freezer: Freezer, name: String) {
        freezerDao.update(freezer.copy(name = name.trim()))
    }

    suspend fun foodCountInFreezer(freezerId: Long): Int {
        val ids = drawerDao.getByFreezer(freezerId).map { it.id }
        return if (ids.isEmpty()) 0 else foodDao.countInDrawers(ids)
    }

    suspend fun deleteFreezer(freezerId: Long) {
        db.withTransaction {
            val ids = drawerDao.getByFreezer(freezerId).map { it.id }
            if (ids.isNotEmpty()) foodDao.deleteByDrawers(ids)
            drawerDao.deleteByFreezer(freezerId)
            freezerDao.delete(freezerId)
        }
        if (_selectedFreezerId.value == freezerId) selectFreezer(-1L)
    }

    suspend fun addDrawer(freezerId: Long, name: String) {
        val position = (drawerDao.getByFreezer(freezerId).maxOfOrNull { it.position } ?: -1) + 1
        drawerDao.insert(Drawer(freezerId = freezerId, name = name.trim(), position = position))
    }

    suspend fun renameDrawer(drawer: Drawer, name: String) {
        drawerDao.update(drawer.copy(name = name.trim()))
    }

    suspend fun foodCountInDrawer(drawerId: Long): Int = foodDao.countInDrawer(drawerId)

    suspend fun deleteDrawer(drawerId: Long) {
        db.withTransaction {
            foodDao.deleteByDrawer(drawerId)
            drawerDao.delete(drawerId)
        }
    }

    /** Moves a drawer one step up (towards the top) or down. */
    suspend fun moveDrawer(freezerId: Long, drawerId: Long, up: Boolean) {
        db.withTransaction {
            val list = drawerDao.getByFreezer(freezerId).toMutableList()
            val index = list.indexOfFirst { it.id == drawerId }
            val target = if (up) index - 1 else index + 1
            if (index < 0 || target !in list.indices) return@withTransaction
            val moved = list.removeAt(index)
            list.add(target, moved)
            list.forEachIndexed { i, d -> if (d.position != i) drawerDao.update(d.copy(position = i)) }
        }
    }

    // ---- Foods ----

    suspend fun getFood(id: Long): Food? = foodDao.getById(id)

    suspend fun addFood(food: Food): Long = foodDao.insert(food.copy(id = 0))

    suspend fun updateFood(food: Food) = foodDao.update(food)

    suspend fun deleteFood(id: Long) = foodDao.delete(id)

    suspend fun duplicateFood(id: Long): Long? {
        val food = foodDao.getById(id) ?: return null
        return foodDao.insert(food.copy(id = 0, dateAdded = System.currentTimeMillis()))
    }

    // ---- Categories ----

    suspend fun addCategory(name: String, color: Long) {
        categoryDao.insert(
            Category(name = name.trim(), color = color, position = categoryDao.maxPosition() + 1)
        )
    }

    suspend fun updateCategory(category: Category) = categoryDao.update(category)

    /** Deletes a category and removes it from every food that used it. */
    suspend fun deleteCategory(id: Long) {
        db.withTransaction {
            foodDao.getAll().filter { id in it.categoryIds }.forEach {
                foodDao.update(it.copy(categoryIds = it.categoryIds - id))
            }
            categoryDao.delete(id)
        }
    }

    // ---- Shopping list ----

    suspend fun addShoppingItem(name: String): Long = shoppingDao.insert(ShoppingItem(name = name.trim()))

    suspend fun setShoppingChecked(item: ShoppingItem, checked: Boolean) =
        shoppingDao.update(item.copy(checked = checked))

    suspend fun deleteShoppingItem(id: Long) = shoppingDao.delete(id)

    suspend fun clearCheckedShopping() = shoppingDao.deleteChecked()

    // ---- Backup ----

    suspend fun exportBackup(): String = db.withTransaction {
        BackupCodec.encode(
            BackupData(
                freezers = freezerDao.getAll(),
                drawers = drawerDao.getAll(),
                categories = categoryDao.getAll(),
                foods = foodDao.getAll(),
                shopping = shoppingDao.getAll()
            )
        )
    }

    /** Replaces ALL data. Throws [IllegalArgumentException] (before touching anything) if invalid. */
    suspend fun importBackup(text: String): String {
        val result = BackupCodec.decode(text)
        replaceAll(result.data)
        return result.summary
    }

    private suspend fun replaceAll(data: BackupData) {
        db.withTransaction {
            foodDao.deleteAll()
            drawerDao.deleteAll()
            freezerDao.deleteAll()
            categoryDao.deleteAll()
            shoppingDao.deleteAll()
            categoryDao.insertAll(data.categories)
            freezerDao.insertAll(data.freezers)
            drawerDao.insertAll(data.drawers)
            foodDao.insertAll(data.foods)
            shoppingDao.insertAll(data.shopping)
        }
        selectFreezer(-1L)
    }

    private companion object {
        const val KEY_SELECTED_FREEZER = "selected_freezer"
    }
}
