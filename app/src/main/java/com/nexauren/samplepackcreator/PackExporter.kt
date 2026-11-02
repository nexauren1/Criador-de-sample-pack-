package com.nexauren.samplepackcreator

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PackExporter {
    data class Result(
        val fileName: String,
        val totalSamples: Int,
        val destination: String,
        val projectId: String
    )

    private data class SourceAudio(
        val name: String,
        val bytes: ByteArray,
        val extension: String,
        val processableWav: ByteArray?
    )

    fun create(
        context: Context,
        packName: String,
        genre: String,
        categories: List<SampleCategory>,
        selectedFiles: Map<String, List<Uri>>,
        generatedFiles: Map<String, List<File>>,
        quantities: Map<String, Int>,
        samplePrefixes: Map<String, String>
    ): Result {
        val safePackName = safeName(packName).ifBlank { "My_Sample_Pack" }
        val workRoot = File(context.cacheDir, "sample-pack-work-" + System.currentTimeMillis())
        val packFolder = File(workRoot, safePackName)
        if (!packFolder.mkdirs() && !packFolder.isDirectory) {
            throw IllegalStateException("Não foi possível preparar a pasta temporária.")
        }

        val counts = linkedMapOf<String, Int>()
        val notes = mutableListOf<String>()
        var total = 0

        categories.forEach { category ->
            val sources = mutableListOf<SourceAudio>()

            selectedFiles[category.id].orEmpty().forEach { uri ->
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                        val data = input.readBytes()
                        if (data.size > 40 * 1024 * 1024) {
                            throw IllegalArgumentException("Um ficheiro excede o limite de 40 MB.")
                        }
                        data
                    } ?: throw IllegalStateException("Não foi possível ler um ficheiro de áudio.")
                    val name = queryDisplayName(context, uri)
                    val extension = name.substringAfterLast('.', "audio").lowercase(Locale.ROOT)
                    val directWav = if (extension == "wav" && WavProcessor.process(bytes, 0) != null) bytes else null
                    val normalized = directWav ?: AudioDecoder.decodeToWav(context, uri)
                    sources += SourceAudio(name, bytes, extension, normalized)
                    if (normalized == null) {
                        notes += name + ": preservado no formato original; este dispositivo não conseguiu descodificá-lo para gerar variações."
                    }
                } catch (exception: Exception) {
                    notes += category.name + ": " + (exception.message ?: "um ficheiro não pôde ser lido")
                }
            }

            generatedFiles[category.id].orEmpty().forEach { file ->
                try {
                    if (file.isFile && file.length() <= 40L * 1024L * 1024L) {
                        val bytes = file.readBytes()
                        val processable = if (WavProcessor.process(bytes, 0) != null) bytes else null
                        sources += SourceAudio(file.name, bytes, file.extension.lowercase(Locale.ROOT), processable)
                    }
                } catch (exception: Exception) {
                    notes += file.name + ": " + (exception.message ?: "um sample gerado não pôde ser lido")
                }
            }

            if (sources.isEmpty()) return@forEach

            val categoryName = safeName(category.name).ifBlank { "Samples" }
            val categoryFolder = File(packFolder, categoryName)
            if (!categoryFolder.mkdirs() && !categoryFolder.isDirectory) {
                throw IllegalStateException("Não foi possível criar a pasta " + category.name)
            }

            val requested = (quantities[category.id] ?: 10).coerceIn(1, 250)
            val samplePrefix = safeName(samplePrefixes[category.id] ?: category.name).ifBlank { categoryName }
            var produced = 0
            var pass = 0
            while (produced < requested && pass < requested) {
                var producedInPass = 0
                sources.forEachIndexed { sourceIndex, source ->
                    if (produced >= requested) return@forEachIndexed
                    val variationIndex = pass * sources.size + sourceIndex
                    val processed = source.processableWav?.let { WavProcessor.process(it, variationIndex) }
                    val outputBytes = processed ?: if (pass == 0) source.bytes else null
                    if (outputBytes != null) {
                        produced++
                        producedInPass++
                        val processedName = processed != null
                        val label = if (processedName) WavProcessor.effectName(variationIndex) else "Original"
                        val extension = if (processedName) "wav" else source.extension.ifBlank { "audio" }
                        val outputName = samplePrefix + "_" + produced.toString().padStart(3, '0') +
                            "_" + label + "." + extension
                        File(categoryFolder, outputName).writeBytes(outputBytes)
                    }
                }
                if (producedInPass == 0) break
                pass++
            }

            if (produced > 0) {
                counts[category.name] = produced
                total += produced
            }
            if (produced < requested) {
                notes += category.name + ": " + produced + " ficheiro(s) criado(s) de " + requested +
                    " pedido(s). Adiciona mais fontes compatíveis para obter mais variações."
            }
        }

        if (total == 0) {
            workRoot.deleteRecursively()
            throw IllegalStateException("Nenhum áudio pôde ser incluído. Seleciona fontes compatíveis ou gera samples primeiro.")
        }

        File(packFolder, "README.txt").writeText(buildReadme(packName, genre, counts, total, notes), Charsets.UTF_8)

        val projectId = StudioLibraryStore.newId()
        val projectRoot = File(context.filesDir, "sample-pack-studio/projects/$projectId")
        val persistentFolder = File(projectRoot, safePackName)
        if (!persistentFolder.parentFile!!.exists() && !persistentFolder.parentFile!!.mkdirs()) {
            workRoot.deleteRecursively()
            throw IllegalStateException("Não foi possível guardar o projeto na biblioteca.")
        }
        packFolder.copyRecursively(persistentFolder, overwrite = true)

        val zipName = safePackName + ".zip"
        val internalZip = File(projectRoot, zipName)
        zipFolder(persistentFolder, internalZip)
        val destination = copyZipToDownloads(context, internalZip, zipName)

        val sampleRecords = persistentFolder.walkTopDown()
            .filter { it.isFile && it.name != "README.txt" }
            .map { file ->
                ProjectSample(
                    name = file.name,
                    category = file.parentFile?.name ?: "Samples",
                    path = file.absolutePath,
                    bytes = file.length()
                )
            }.toList()

        StudioLibraryStore.saveProject(
            context,
            SavedProject(
                id = projectId,
                title = packName,
                genre = genre,
                createdAt = System.currentTimeMillis(),
                totalSamples = total,
                folderPath = persistentFolder.absolutePath,
                zipPath = internalZip.absolutePath,
                samples = sampleRecords
            )
        )
        workRoot.deleteRecursively()
        return Result(zipName, total, destination, projectId)
    }

    private fun buildReadme(
        packName: String,
        genre: String,
        counts: Map<String, Int>,
        total: Int,
        notes: List<String>
    ): String {
        val builder = StringBuilder()
        builder.appendLine(packName)
        builder.appendLine("=".repeat(minOf(packName.length, 60)))
        builder.appendLine()
        builder.appendLine("GENRE")
        builder.appendLine(genre.ifBlank { "Not specified" })
        builder.appendLine()
        builder.appendLine("PACK CONTENTS")
        counts.forEach { (category, count) ->
            builder.appendLine("- " + category + ": " + count + " samples")
        }
        builder.appendLine()
        builder.appendLine("TOTAL")
        builder.appendLine(total.toString() + " audio files")
        builder.appendLine()
        builder.appendLine("AUDIO FORMATS")
        builder.appendLine("- Generated and processed samples are exported as 16-bit PCM WAV.")
        builder.appendLine("- Source files that cannot be decoded on this Android device are preserved in their original format.")
        builder.appendLine()
        builder.appendLine("PROCESSING")
        builder.appendLine("- Original audio can be transformed using punch, low-pass tone shaping, distortion, reverse, delay, bit reduction, high-pass and saturation variations.")
        builder.appendLine("- Synthesized audio is generated locally by Sample Pack Creator.")
        builder.appendLine()
        builder.appendLine("USAGE")
        builder.appendLine("Check the applicable licence and usage rights of every source recording before distributing this pack.")
        builder.appendLine("Do not redistribute third-party recordings unless you have the necessary permission.")
        builder.appendLine()
        builder.appendLine("CREATED")
        builder.appendLine(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        builder.appendLine()
        builder.appendLine("CREATOR")
        builder.appendLine("Created with Sample Pack Creator")
        if (notes.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("PROCESSING NOTES")
            notes.forEach { builder.appendLine("- " + it) }
        }
        return builder.toString()
    }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        var name = "Audio_Sample.wav"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) name = cursor.getString(index) ?: name
        }
        return name
    }

    private fun safeName(value: String): String =
        value.trim()
            .replace(Regex("[^A-Za-z0-9 _.-]"), "")
            .replace(Regex("\\s+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_', '.', ' ')
            .take(64)

    private fun zipFolder(folder: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            val rootName = folder.name + "/"
            folder.walkTopDown().filter { it.isFile }.forEach { file ->
                val relative = file.relativeTo(folder).invariantSeparatorsPath
                zip.putNextEntry(ZipEntry(rootName + relative))
                FileInputStream(file).use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    private fun copyZipToDownloads(context: Context, source: File, zipName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, zipName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Sample Pack Creator")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("O Android não permitiu criar o ZIP na pasta Downloads.")
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    FileInputStream(source).use { input -> input.copyTo(output) }
                } ?: throw IllegalStateException("Não foi possível gravar o ficheiro ZIP.")
                val completed = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                context.contentResolver.update(uri, completed, null, null)
                return "Downloads/Sample Pack Creator"
            } catch (exception: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw exception
            }
        }

        val destinationFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Sample Pack Creator")
        if (!destinationFolder.exists()) destinationFolder.mkdirs()
        val destination = File(destinationFolder, zipName)
        source.copyTo(destination, overwrite = true)
        return destination.absolutePath
    }
}
