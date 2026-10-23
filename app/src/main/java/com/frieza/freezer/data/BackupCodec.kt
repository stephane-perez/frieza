package com.frieza.freezer.data

import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val freezers: List<Freezer>,
    val drawers: List<Drawer>,
    val categories: List<Category>,
    val foods: List<Food>,
    val shopping: List<ShoppingItem>
)

data class DecodedBackup(val data: BackupData, val summary: String)

/**
 * JSON backup format used to export and restore all the app's data.
 */
object BackupCodec {

    fun encode(data: BackupData): String {
        val root = JSONObject()
        root.put("app", "frieza")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("categories", JSONArray().also { arr ->
            data.categories.forEach {
                arr.put(JSONObject().put("id", it.id).put("name", it.name).put("color", it.color).put("position", it.position))
            }
        })
        root.put("freezers", JSONArray().also { arr ->
            data.freezers.forEach {
                arr.put(JSONObject().put("id", it.id).put("name", it.name).put("position", it.position))
            }
        })
        root.put("drawers", JSONArray().also { arr ->
            data.drawers.forEach {
                arr.put(
                    JSONObject().put("id", it.id).put("freezerId", it.freezerId)
                        .put("name", it.name).put("position", it.position)
                )
            }
        })
        root.put("foods", JSONArray().also { arr ->
            data.foods.forEach { f ->
                val o = JSONObject()
                o.put("id", f.id)
                o.put("name", f.name)
                o.put("drawerId", f.drawerId)
                o.put("categoryIds", JSONArray(f.categoryIds))
                f.unitCount?.let { o.put("unitCount", it) }
                f.weightGrams?.let { o.put("weightGrams", it) }
                f.portions?.let { o.put("portions", it) }
                f.freezeDate?.let { o.put("freezeDate", it) }
                f.expirationDate?.let { o.put("expirationDate", it) }
                o.put("notes", f.notes)
                o.put("dateAdded", f.dateAdded)
                arr.put(o)
            }
        })
        root.put("shopping", JSONArray().also { arr ->
            data.shopping.forEach {
                arr.put(JSONObject().put("id", it.id).put("name", it.name).put("checked", it.checked))
            }
        })
        return root.toString(1)
    }

    fun decode(text: String): DecodedBackup {
        val root = try {
            JSONObject(text.trim())
        } catch (e: Exception) {
            throw IllegalArgumentException("Ce fichier n'est pas une sauvegarde valide (JSON illisible).")
        }
        return when {
            root.optString("app") == "frieza" -> decodeFrieza(root)
            else -> throw IllegalArgumentException("Format de sauvegarde non reconnu.")
        }
    }

    // ---------------------------------------------------------------- Frieza

    private fun decodeFrieza(root: JSONObject): DecodedBackup {
        try {
            val categories = root.arr("categories").objects().map {
                Category(it.getLong("id"), it.getString("name"), it.getLong("color"), it.optInt("position", 0))
            }
            val freezers = root.arr("freezers").objects().map {
                Freezer(it.getLong("id"), it.getString("name"), it.optInt("position", 0))
            }
            val freezerIds = freezers.map { it.id }.toSet()
            val drawers = root.arr("drawers").objects()
                .map { Drawer(it.getLong("id"), it.getLong("freezerId"), it.getString("name"), it.optInt("position", 0)) }
                .filter { it.freezerId in freezerIds }
            val drawerIds = drawers.map { it.id }.toSet()
            val foods = root.arr("foods").objects().map {
                Food(
                    id = it.getLong("id"),
                    name = it.getString("name"),
                    drawerId = it.getLong("drawerId"),
                    categoryIds = it.arr("categoryIds").let { a -> (0 until a.length()).map { i -> a.getLong(i) } },
                    unitCount = it.intOrNull("unitCount"),
                    weightGrams = it.intOrNull("weightGrams"),
                    portions = it.intOrNull("portions"),
                    freezeDate = it.longOrNull("freezeDate"),
                    expirationDate = it.longOrNull("expirationDate"),
                    notes = it.optString("notes", ""),
                    dateAdded = it.optLong("dateAdded", System.currentTimeMillis())
                )
            }.filter { it.drawerId in drawerIds }
            val categoryIds = categories.map { it.id }.toSet()
            val cleanFoods = foods.map { it.copy(categoryIds = it.categoryIds.filter { c -> c in categoryIds }) }
            val shopping = root.arr("shopping").objects().map {
                ShoppingItem(it.getLong("id"), it.getString("name"), it.optBoolean("checked", false))
            }
            val data = BackupData(freezers, drawers, categories, cleanFoods, shopping)
            return DecodedBackup(
                data,
                "${freezers.size} congélateur(s), ${cleanFoods.size} aliment(s), ${categories.size} catégorie(s)"
            )
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Sauvegarde Frieza incomplète ou corrompue.")
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun JSONObject.arr(key: String): JSONArray = optJSONArray(key) ?: JSONArray()

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONObject.intOrNull(key: String): Int? = if (has(key) && !isNull(key)) optInt(key) else null

    private fun JSONObject.longOrNull(key: String): Long? = if (has(key) && !isNull(key)) optLong(key) else null
}
