package com.frieza.freezer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FreezerConfigDao {
    @Query("SELECT * FROM freezer_config WHERE id = 0")
    fun getConfig(): Flow<FreezerConfig?>

    @Query("SELECT * FROM freezer_config WHERE id = 0")
    suspend fun getConfigOnce(): FreezerConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: FreezerConfig)
}
