package com.frieza.freezer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FreezerDao {
    @Query("SELECT * FROM freezers ORDER BY position ASC, id ASC")
    fun observeAll(): Flow<List<Freezer>>

    @Query("SELECT * FROM freezers ORDER BY position ASC, id ASC")
    suspend fun getAll(): List<Freezer>

    @Insert
    suspend fun insert(freezer: Freezer): Long

    @Insert
    suspend fun insertAll(freezers: List<Freezer>)

    @Update
    suspend fun update(freezer: Freezer)

    @Query("DELETE FROM freezers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM freezers")
    suspend fun deleteAll()
}

@Dao
interface DrawerDao {
    @Query("SELECT * FROM drawers ORDER BY position ASC, id ASC")
    fun observeAll(): Flow<List<Drawer>>

    @Query("SELECT * FROM drawers ORDER BY position ASC, id ASC")
    suspend fun getAll(): List<Drawer>

    @Query("SELECT * FROM drawers WHERE freezerId = :freezerId ORDER BY position ASC, id ASC")
    suspend fun getByFreezer(freezerId: Long): List<Drawer>

    @Insert
    suspend fun insert(drawer: Drawer): Long

    @Insert
    suspend fun insertAll(drawers: List<Drawer>)

    @Update
    suspend fun update(drawer: Drawer)

    @Query("DELETE FROM drawers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM drawers WHERE freezerId = :freezerId")
    suspend fun deleteByFreezer(freezerId: Long)

    @Query("DELETE FROM drawers")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY position ASC, id ASC")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY position ASC, id ASC")
    suspend fun getAll(): List<Category>

    @Query("SELECT COALESCE(MAX(position), -1) FROM categories")
    suspend fun maxPosition(): Int

    @Insert
    suspend fun insert(category: Category): Long

    @Insert
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Food>>

    @Query("SELECT * FROM foods")
    suspend fun getAll(): List<Food>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): Food?

    @Query("SELECT COUNT(*) FROM foods WHERE drawerId = :drawerId")
    suspend fun countInDrawer(drawerId: Long): Int

    @Query("SELECT COUNT(*) FROM foods WHERE drawerId IN (:drawerIds)")
    suspend fun countInDrawers(drawerIds: List<Long>): Int

    @Insert
    suspend fun insert(food: Food): Long

    @Insert
    suspend fun insertAll(foods: List<Food>)

    @Update
    suspend fun update(food: Food)

    @Query("DELETE FROM foods WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM foods WHERE drawerId = :drawerId")
    suspend fun deleteByDrawer(drawerId: Long)

    @Query("DELETE FROM foods WHERE drawerId IN (:drawerIds)")
    suspend fun deleteByDrawers(drawerIds: List<Long>)

    @Query("DELETE FROM foods")
    suspend fun deleteAll()
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY checked ASC, id ASC")
    fun observeAll(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items")
    suspend fun getAll(): List<ShoppingItem>

    @Insert
    suspend fun insert(item: ShoppingItem): Long

    @Insert
    suspend fun insertAll(items: List<ShoppingItem>)

    @Update
    suspend fun update(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM shopping_items WHERE checked = 1")
    suspend fun deleteChecked()

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()
}
