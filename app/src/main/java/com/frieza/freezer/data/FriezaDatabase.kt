package com.frieza.freezer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Food::class, FreezerConfig::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class FriezaDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun freezerConfigDao(): FreezerConfigDao

    companion object {
        @Volatile
        private var INSTANCE: FriezaDatabase? = null

        fun getInstance(context: Context): FriezaDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FriezaDatabase::class.java,
                    "frieza.db"
                ).build().also { INSTANCE = it }
            }
    }
}
