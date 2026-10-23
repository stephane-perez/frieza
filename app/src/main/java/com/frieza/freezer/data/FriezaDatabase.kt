package com.frieza.freezer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Freezer::class, Drawer::class, Category::class, Food::class, ShoppingItem::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FriezaDatabase : RoomDatabase() {
    abstract fun freezerDao(): FreezerDao
    abstract fun drawerDao(): DrawerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun foodDao(): FoodDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile
        private var INSTANCE: FriezaDatabase? = null

        fun getInstance(context: Context): FriezaDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FriezaDatabase::class.java,
                    "frieza.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            seedCategories(db)
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }

        private fun seedCategories(db: SupportSQLiteDatabase) {
            DefaultCategories.all.forEachIndexed { index, (name, color) ->
                db.execSQL(
                    "INSERT INTO categories (name, color, position) VALUES (?, ?, ?)",
                    arrayOf<Any?>(name, color, index.toLong())
                )
            }
        }

        /**
         * v1 had one implicit freezer (floorCount) and foods with `floor` + a fixed category enum.
         * v2 has freezers, named drawers, user categories and richer quantities/dates.
         * Existing data is preserved: floor N becomes drawer "Étage N" of freezer "Congélateur".
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `freezers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `drawers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `freezerId` INTEGER NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `color` INTEGER NOT NULL, `position` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `shopping_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `checked` INTEGER NOT NULL)")

                seedCategories(db)

                var floorCount = 0
                db.query("SELECT floorCount FROM freezer_config WHERE id = 0").use { cursor ->
                    if (cursor.moveToFirst()) floorCount = cursor.getInt(0)
                }
                if (floorCount > 0) {
                    db.execSQL("INSERT INTO freezers (id, name, position) VALUES (1, 'Congélateur', 0)")
                    for (floor in 1..floorCount) {
                        db.execSQL(
                            "INSERT INTO drawers (id, freezerId, name, position) VALUES (?, 1, ?, ?)",
                            arrayOf<Any?>(floor.toLong(), "Étage $floor", (floorCount - floor).toLong())
                        )
                    }
                }

                db.execSQL("ALTER TABLE foods RENAME TO foods_old")
                db.execSQL("CREATE TABLE IF NOT EXISTS `foods` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `drawerId` INTEGER NOT NULL, `categoryIds` TEXT NOT NULL, `unitCount` INTEGER, `weightGrams` INTEGER, `portions` INTEGER, `freezeDate` INTEGER, `expirationDate` INTEGER, `notes` TEXT NOT NULL, `dateAdded` INTEGER NOT NULL)")
                db.execSQL(
                    """
                    INSERT INTO foods (id, name, drawerId, categoryIds, unitCount, weightGrams, portions, freezeDate, expirationDate, notes, dateAdded)
                    SELECT id, name, floor,
                        CASE category
                            WHEN 'LEGUMES' THEN '1'
                            WHEN 'FRUITS' THEN '2'
                            WHEN 'PLATS_COMPLETS' THEN '3'
                            WHEN 'VIANDES' THEN '4'
                            WHEN 'POISSONS' THEN '5'
                            WHEN 'CONDIMENTS' THEN '6'
                            WHEN 'PAIN_PATISSERIE' THEN '7'
                            WHEN 'DESSERTS_GLACES' THEN '8'
                            ELSE '9'
                        END,
                        NULL,
                        CASE WHEN quantityType = 'POIDS' THEN quantityValue END,
                        CASE WHEN quantityType = 'PORTIONS' THEN quantityValue END,
                        (dateAdded / 86400000) * 86400000,
                        expirationDate, '', dateAdded
                    FROM foods_old
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE foods_old")
                db.execSQL("DROP TABLE IF EXISTS freezer_config")
            }
        }
    }
}
