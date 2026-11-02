package com.nexauren.samplepackcreator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class ProjectSample(
    val name: String,
    val category: String,
    val path: String,
    val bytes: Long
)

data class SavedProject(
    val id: String,
    val title: String,
    val genre: String,
    val createdAt: Long,
    val totalSamples: Int,
    val folderPath: String,
    val zipPath: String,
    val samples: List<ProjectSample>
)

data class GeneratedSample(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val soundType: String,
    val name: String,
    val path: String,
    val createdAt: Long
)

object StudioLibraryStore {
    private fun root(context: Context) = File(context.filesDir, "sample-pack-studio").apply { mkdirs() }
    private fun projectIndex(context: Context) = File(root(context), "projects.json")
    private fun generatedIndex(context: Context) = File(root(context), "generated.json")

    fun projects(context: Context): List<SavedProject> {
        val file = projectIndex(context)
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val sampleArray = item.optJSONArray("samples") ?: JSONArray()
                    val samples = buildList {
                        for (sampleIndex in 0 until sampleArray.length()) {
                            val sample = sampleArray.getJSONObject(sampleIndex)
                            add(ProjectSample(
                                sample.optString("name"),
                                sample.optString("category"),
                                sample.optString("path"),
                                sample.optLong("bytes")
                            ))
                        }
                    }
                    val project = SavedProject(
                        item.optString("id"),
                        item.optString("title"),
                        item.optString("genre"),
                        item.optLong("createdAt"),
                        item.optInt("totalSamples"),
                        item.optString("folderPath"),
                        item.optString("zipPath"),
                        samples
                    )
                    if (File(project.folderPath).exists() && File(project.zipPath).exists()) add(project)
                }
            }.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveProject(context: Context, project: SavedProject) {
        val list = projects(context).filterNot { it.id == project.id } + project
        val array = JSONArray()
        list.sortedByDescending { it.createdAt }.forEach { item ->
            val sampleArray = JSONArray()
            item.samples.forEach { sample ->
                sampleArray.put(JSONObject()
                    .put("name", sample.name)
                    .put("category", sample.category)
                    .put("path", sample.path)
                    .put("bytes", sample.bytes))
            }
            array.put(JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("genre", item.genre)
                .put("createdAt", item.createdAt)
                .put("totalSamples", item.totalSamples)
                .put("folderPath", item.folderPath)
                .put("zipPath", item.zipPath)
                .put("samples", sampleArray))
        }
        projectIndex(context).writeText(array.toString())
    }

    fun deleteProject(context: Context, project: SavedProject) {
        File(project.folderPath).parentFile?.deleteRecursively()
        val array = JSONArray()
        projects(context).filterNot { it.id == project.id }.forEach { item ->
            val sampleArray = JSONArray()
            item.samples.forEach { sample ->
                sampleArray.put(JSONObject()
                    .put("name", sample.name)
                    .put("category", sample.category)
                    .put("path", sample.path)
                    .put("bytes", sample.bytes))
            }
            array.put(JSONObject()
                .put("id", item.id).put("title", item.title).put("genre", item.genre)
                .put("createdAt", item.createdAt).put("totalSamples", item.totalSamples)
                .put("folderPath", item.folderPath).put("zipPath", item.zipPath)
                .put("samples", sampleArray))
        }
        projectIndex(context).writeText(array.toString())
    }

    fun generatedSamples(context: Context): List<GeneratedSample> {
        val file = generatedIndex(context)
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val sample = GeneratedSample(
                        item.optString("id"),
                        item.optString("categoryId"),
                        item.optString("categoryName"),
                        item.optString("soundType"),
                        item.optString("name"),
                        item.optString("path"),
                        item.optLong("createdAt")
                    )
                    if (File(sample.path).isFile) add(sample)
                }
            }.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addGenerated(context: Context, sample: GeneratedSample) {
        val items = generatedSamples(context).filterNot { it.id == sample.id } + sample
        val array = JSONArray()
        items.sortedByDescending { it.createdAt }.forEach { item ->
            array.put(JSONObject()
                .put("id", item.id)
                .put("categoryId", item.categoryId)
                .put("categoryName", item.categoryName)
                .put("soundType", item.soundType)
                .put("name", item.name)
                .put("path", item.path)
                .put("createdAt", item.createdAt))
        }
        generatedIndex(context).writeText(array.toString())
    }

    fun deleteGenerated(context: Context, sample: GeneratedSample) {
        File(sample.path).delete()
        val array = JSONArray()
        generatedSamples(context).filterNot { it.id == sample.id }.forEach { item ->
            array.put(JSONObject()
                .put("id", item.id)
                .put("categoryId", item.categoryId)
                .put("categoryName", item.categoryName)
                .put("soundType", item.soundType)
                .put("name", item.name)
                .put("path", item.path)
                .put("createdAt", item.createdAt))
        }
        generatedIndex(context).writeText(array.toString())
    }

    fun newId(): String = UUID.randomUUID().toString()
}
