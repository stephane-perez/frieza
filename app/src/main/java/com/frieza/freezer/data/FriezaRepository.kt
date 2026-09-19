package com.frieza.freezer.data

import kotlinx.coroutines.flow.Flow

class FriezaRepository(private val db: FriezaDatabase) {

    val foods: Flow<List<Food>> = db.foodDao().getAll()
    val config: Flow<FreezerConfig?> = db.freezerConfigDao().getConfig()

    suspend fun getConfigOnce(): FreezerConfig? = db.freezerConfigDao().getConfigOnce()

    suspend fun saveConfig(floorCount: Int) {
        db.freezerConfigDao().upsert(FreezerConfig(id = 0, floorCount = floorCount))
        db.foodDao().deleteFoodsAboveFloor(floorCount)
    }

    suspend fun getFood(id: Long): Food? = db.foodDao().getById(id)

    suspend fun addFood(food: Food): Long = db.foodDao().insert(food)

    suspend fun updateFood(food: Food) = db.foodDao().update(food)

    suspend fun deleteFood(food: Food) = db.foodDao().delete(food)
}
