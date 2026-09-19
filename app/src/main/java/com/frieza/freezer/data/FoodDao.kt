package com.frieza.freezer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY floor ASC, name ASC")
    fun getAll(): Flow<List<Food>>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): Food?

    @Insert
    suspend fun insert(food: Food): Long

    @Update
    suspend fun update(food: Food)

    @Delete
    suspend fun delete(food: Food)

    @Query("DELETE FROM foods WHERE floor > :maxFloor")
    suspend fun deleteFoodsAboveFloor(maxFloor: Int)
}
