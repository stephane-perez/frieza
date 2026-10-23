package com.frieza.freezer.data

import com.frieza.freezer.util.parseIsoDate
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
 * JSON backup format. Reads Frieza backups and also the export of the Frizor app
 * (`{"format":"frizor-local", "frizors":…, "cat":…}`), so a Frizor collection can be brought over.
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
            root.has("frizors") -> decodeFrizor(root)
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

    // ---------------------------------------------------------------- Frizor

    private fun decodeFrizor(root: JSONObject): DecodedBackup {
        try {
            // Categories (sub-categories get "Parent › Child" names, since Frieza categories are flat).
            val rawCats = root.optJSONArray("cat")?.objects().orEmpty()
            val byId = rawCats.associateBy { it.optLong("idcategorie") }
            val categories = rawCats.mapIndexed { index, c ->
                val id = c.optLong("idcategorie")
                val parent = byId[c.optLong("idcatparent", 0L)]
                val label = c.optString("libelle")
                Category(
                    id = id,
                    name = if (parent != null) "${parent.optString("libelle")} › $label" else label,
                    color = parseColor(c.optString("couleur_b"), parent?.optString("couleur_b")),
                    position = index
                )
            }.filter { it.id > 0 }.let { list ->
                if (list.isEmpty()) {
                    DefaultCategories.all.mapIndexed { i, (n, col) -> Category((i + 1).toLong(), n, col, i) }
                } else list
            }
            val knownCats = categories.map { it.id }.toSet()

            val shoppingId = root.optLong("ldc", -1L)
            val frizors = root.getJSONObject("frizors")
            val freezers = mutableListOf<Freezer>()
            val drawers = mutableListOf<Drawer>()
            val foods = mutableListOf<Food>()
            val shopping = mutableListOf<ShoppingItem>()
            var nextDrawerId = 1L

            for (fid in frizors.keys().asSequence().sortedBy { it.toLongOrNull() ?: 0L }) {
                val f = frizors.getJSONObject(fid)
                val isShopping = f.optInt("etat") == 5 || fid.toLongOrNull() == shoppingId
                val tiroirs = f.optJSONObject("tiroirs") ?: JSONObject()
                val tiroirKeys = tiroirs.keys().asSequence().sortedBy { it.toLongOrNull() ?: 0L }.toList()

                if (isShopping) {
                    for (tid in tiroirKeys) {
                        val contenu = tiroirs.getJSONObject(tid).optJSONObject("contenu") ?: continue
                        for (pid in contenu.keys().asSequence().sortedBy { it.toLongOrNull() ?: 0L }) {
                            val label = contenu.getJSONObject(pid).optString("libelle")
                            if (label.isNotBlank()) shopping += ShoppingItem(name = label)
                        }
                    }
                    continue
                }

                val freezerId = (freezers.size + 1).toLong()
                freezers += Freezer(freezerId, f.optString("nom").ifBlank { "Congélateur ${freezerId}" }, freezers.size)
                tiroirKeys.forEachIndexed { index, tid ->
                    val t = tiroirs.getJSONObject(tid)
                    val drawerId = nextDrawerId++
                    drawers += Drawer(drawerId, freezerId, t.optString("nom").ifBlank { "Tiroir ${index + 1}" }, index)
                    val contenu = t.optJSONObject("contenu") ?: JSONObject()
                    for (pid in contenu.keys().asSequence().sortedBy { it.toLongOrNull() ?: 0L }) {
                        val p = contenu.getJSONObject(pid)
                        val cats = (p.optJSONArray("cat1").longs() + p.optJSONArray("cat2").longs())
                            .filter { it in knownCats }.distinct()
                        val freeze = parseIsoDate(p.optString("datec"))
                        foods += Food(
                            name = p.optString("libelle").ifBlank { "Sans nom" },
                            drawerId = drawerId,
                            categoryIds = cats,
                            unitCount = p.numberOrNull("nb"),
                            weightGrams = p.numberOrNull("poids"),
                            portions = p.numberOrNull("nbportions"),
                            freezeDate = freeze,
                            expirationDate = parseIsoDate(p.optString("dlc")),
                            notes = p.optString("description", ""),
                            dateAdded = freeze ?: System.currentTimeMillis()
                        )
                    }
                }
            }
            val data = BackupData(freezers, drawers, categories, foods, shopping)
            return DecodedBackup(
                data,
                "Import Frizor : ${freezers.size} congélateur(s), ${foods.size} aliment(s), " +
                    "${categories.size} catégorie(s), ${shopping.size} article(s) de courses"
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Sauvegarde Frizor illisible.")
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun parseColor(hex: String?, fallbackHex: String? = null): Long {
        for (candidate in listOf(hex, fallbackHex)) {
            val h = candidate?.trim()?.removePrefix("#") ?: continue
            if (h.length == 6) h.toLongOrNull(16)?.let { return 0xFF000000L or it }
        }
        return 0xFF78909CL
    }

    private fun JSONObject.arr(key: String): JSONArray = optJSONArray(key) ?: JSONArray()

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONArray?.longs(): List<Long> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { i -> optLong(i, -1L).takeIf { it > 0 } }

    private fun JSONObject.intOrNull(key: String): Int? = if (has(key) && !isNull(key)) optInt(key) else null

    private fun JSONObject.longOrNull(key: String): Long? = if (has(key) && !isNull(key)) optLong(key) else null

    /** Number that may be an int, a double or a numeric string; zero or negative counts as "not filled". */
    private fun JSONObject.numberOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        val v = optDouble(key, Double.NaN)
        return if (v.isNaN() || v <= 0.0) null else v.toInt()
    }
}
