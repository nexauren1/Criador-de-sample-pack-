package com.nexauren.samplepackcreator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SampleCategory(
    val id: String,
    val name: String,
    val icon: String,
    val color: Int
)

class CategoryStore(context: Context) {
    private val preferences = context.getSharedPreferences("sample_pack_creator", Context.MODE_PRIVATE)

    fun load(): List<SampleCategory> {
        val raw = preferences.getString("categories", null) ?: return defaults()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        SampleCategory(
                            id = item.optString("id", UUID.randomUUID().toString()),
                            name = item.optString("name", "New Category"),
                            icon = item.optString("icon", "🎛️"),
                            color = item.optInt("color", 0xFF8B5CF6.toInt())
                        )
                    )
                }
            }.ifEmpty { defaults() }
        } catch (_: Exception) {
            defaults()
        }
    }

    fun save(categories: List<SampleCategory>) {
        val array = JSONArray()
        categories.forEach { category ->
            array.put(
                JSONObject()
                    .put("id", category.id)
                    .put("name", category.name)
                    .put("icon", category.icon)
                    .put("color", category.color)
            )
        }
        preferences.edit().putString("categories", array.toString()).apply()
    }

    private fun defaults() = listOf(
        SampleCategory("kick", "Kick", "🥁", 0xFF9673FF.toInt()),
        SampleCategory("clap", "Clap", "👏", 0xFF26C6DA.toInt()),
        SampleCategory("snare", "Snare", "🥁", 0xFFFF9F68.toInt()),
        SampleCategory("hihat", "Hi-Hat", "🎚️", 0xFF59DDAA.toInt()),
        SampleCategory("percussion", "Percussion", "🪘", 0xFFFFC857.toInt()),
        SampleCategory("bass", "Bass", "🔊", 0xFF6A9CFF.toInt()),
        SampleCategory("synth", "Synth", "🎹", 0xFFE58CFF.toInt()),
        SampleCategory("fx", "FX", "✨", 0xFFFF7396.toInt())
    )
}
