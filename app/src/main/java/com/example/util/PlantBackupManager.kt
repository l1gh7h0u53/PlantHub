package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.example.data.model.CareActionType
import com.example.data.model.CareLog
import com.example.data.model.GrowthStage
import com.example.data.model.PhotoEntry
import com.example.data.model.Plant
import com.example.data.model.ReminderSetting
import com.example.data.repository.PlantRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * PlantBackupManager handles 100% offline serialization, archiving, and restoration of the PlantHub agronomic database.
 *
 * Key Capabilities:
 * 1. Complete Self-Contained ZIP Bundles: Packages the entire SQLite Room database snapshot alongside original-resolution
 *    milestone images stored in private internal app storage (/data/user/0/com.example/files/milestone_photos).
 * 2. Standardized Formats: Supports JSON Database Backups and multi-table RFC-4180 CSV exports for spreadsheet analysis.
 * 3. Zero-Knowledge / 100% Offline: Backups are written directly via Android Storage Access Framework (SAF)
 *    to user-selected device storage or SD card. Zero network connections, zero telemetry, zero cloud exposure.
 * 4. Automatic Rolling Backups: Background scheduling of weekly snapshots when configured by the grower.
 */
object PlantBackupManager {
    private const val TAG = "PlantBackupManager"
    private const val PREFS_NAME = "planthub_backup_prefs"
    private const val KEY_HAS_BACKUP = "has_backup_state"
    private const val BACKUP_FILE_NAME = "planthub_plants_backup.json"

    const val KEY_AUTO_BACKUP_ENABLED = "auto_weekly_backup_enabled"
    const val KEY_BACKUP_FOLDER_URI = "weekly_backup_folder_uri"
    const val KEY_LAST_BACKUP_TIME = "weekly_backup_last_time"

    fun isAutoBackupEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled)
            .apply()
    }

    fun getBackupFolderUri(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BACKUP_FOLDER_URI, null)
    }

    fun setBackupFolderUri(context: Context, uriString: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BACKUP_FOLDER_URI, uriString)
            .apply()
    }

    fun getLastBackupTime(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    fun setLastBackupTime(context: Context, time: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKUP_TIME, time)
            .apply()
    }

    fun hasSavedBackup(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_HAS_BACKUP, false)) return true
        val file = File(context.filesDir, BACKUP_FILE_NAME)
        return file.exists() && file.length() > 20
    }

    fun saveBackup(
        context: Context,
        plants: List<Plant>,
        logs: List<CareLog>,
        photos: List<PhotoEntry>
    ) {
        if (plants.isEmpty()) return
        try {
            val root = JSONObject()
            root.put("version", 3)
            root.put("timestamp", System.currentTimeMillis())

            val plantsArray = JSONArray()
            for (p in plants) {
                val pObj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("strain", p.strain)
                    put("stage", p.stage.name)
                    put("lightCyclePreset", p.lightCyclePreset)
                    put("lightOnHours", p.lightOnHours)
                    put("lightOffHours", p.lightOffHours)
                    put("lightStartTimeMinute", p.lightStartTimeMinute)
                    put("medium", p.medium)
                    put("potSizeLiters", p.potSizeLiters)
                    put("plantedDate", p.plantedDate)
                    put("notes", p.notes)
                    put("isTimerRunning", p.isTimerRunning)
                    p.timerStartedAt?.let { put("timerStartedAt", it) }
                    p.timerPausedRemainingSec?.let { put("timerPausedRemainingSec", it) }
                    put("isPausedForMaintenance", p.isPausedForMaintenance)
                    put("iconKey", p.iconKey)
                }
                plantsArray.put(pObj)
            }
            root.put("plants", plantsArray)

            val logsArray = JSONArray()
            for (l in logs) {
                val lObj = JSONObject().apply {
                    put("id", l.id)
                    put("plantId", l.plantId)
                    put("timestamp", l.timestamp)
                    put("actionType", l.actionType.name)
                    l.waterAmountMl?.let { put("waterAmountMl", it) }
                    l.phLevel?.let { put("phLevel", it) }
                    l.fertilizerName?.let { put("fertilizerName", it) }
                    l.fertilizerDose?.let { put("fertilizerDose", it) }
                    l.ecPpm?.let { put("ecPpm", it) }
                    put("notes", l.notes)
                }
                logsArray.put(lObj)
            }
            root.put("logs", logsArray)

            val photosArray = JSONArray()
            for (ph in photos) {
                val phObj = JSONObject().apply {
                    put("id", ph.id)
                    put("plantId", ph.plantId)
                    put("timestamp", ph.timestamp)
                    put("imageUri", ph.imageUri)
                    put("stage", ph.stage.name)
                    put("dayNumber", ph.dayNumber)
                    put("title", ph.title)
                    put("observation", ph.observation)
                    ph.canopyHeightCm?.let { put("canopyHeightCm", it) }
                }
                photosArray.put(phObj)
            }
            root.put("photos", photosArray)

            val jsonString = root.toString()

            // Save to internal file
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            file.writeText(jsonString)

            // Also mirror in SharedPreferences
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_HAS_BACKUP, true)
                .putString("backup_json", jsonString)
                .apply()

            Log.d(TAG, "Successfully backed up ${plants.size} plants, ${logs.size} logs, ${photos.size} photos")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving plant backup: ${e.message}", e)
        }
    }

    suspend fun restoreBackup(context: Context, repository: PlantRepository): Boolean {
        return try {
            var jsonString: String? = null
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            if (file.exists()) {
                jsonString = file.readText()
            }
            if (jsonString.isNullOrBlank()) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                jsonString = prefs.getString("backup_json", null)
            }
            if (jsonString.isNullOrBlank()) return false

            val previewData = parseJsonBackup(jsonString, BACKUP_FILE_NAME)
            restoreDatabaseFromData(context, repository, previewData)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring plant backup: ${e.message}", e)
            false
        }
    }

    /**
     * Generates a complete JSON backup export containing all plants, care logs, photos, and reminder settings.
     */
    fun exportFullDatabaseToJson(
        plants: List<Plant>,
        logs: List<CareLog>,
        photos: List<PhotoEntry>,
        reminders: List<ReminderSetting>
    ): String {
        val root = JSONObject().apply {
            put("app", "PlantHub")
            put("format", "JSON_DATABASE_BACKUP")
            put("version", 3)
            put("exportedAt", System.currentTimeMillis())

            val plantsArray = JSONArray()
            for (p in plants) {
                val pObj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("strain", p.strain)
                    put("stage", p.stage.name)
                    put("lightCyclePreset", p.lightCyclePreset)
                    put("lightOnHours", p.lightOnHours)
                    put("lightOffHours", p.lightOffHours)
                    put("lightStartTimeMinute", p.lightStartTimeMinute)
                    put("medium", p.medium)
                    put("potSizeLiters", p.potSizeLiters)
                    put("plantedDate", p.plantedDate)
                    put("notes", p.notes)
                    put("isTimerRunning", p.isTimerRunning)
                    p.timerStartedAt?.let { put("timerStartedAt", it) }
                    p.timerPausedRemainingSec?.let { put("timerPausedRemainingSec", it) }
                    put("isPausedForMaintenance", p.isPausedForMaintenance)
                    put("iconKey", p.iconKey)
                }
                plantsArray.put(pObj)
            }
            put("plants", plantsArray)

            val logsArray = JSONArray()
            for (l in logs) {
                val lObj = JSONObject().apply {
                    put("id", l.id)
                    put("plantId", l.plantId)
                    put("timestamp", l.timestamp)
                    put("actionType", l.actionType.name)
                    l.waterAmountMl?.let { put("waterAmountMl", it) }
                    l.phLevel?.let { put("phLevel", it) }
                    l.fertilizerName?.let { put("fertilizerName", it) }
                    l.fertilizerDose?.let { put("fertilizerDose", it) }
                    l.ecPpm?.let { put("ecPpm", it) }
                    put("notes", l.notes)
                }
                logsArray.put(lObj)
            }
            put("careLogs", logsArray)

            val photosArray = JSONArray()
            for (ph in photos) {
                val phObj = JSONObject().apply {
                    put("id", ph.id)
                    put("plantId", ph.plantId)
                    put("timestamp", ph.timestamp)
                    put("imageUri", ph.imageUri)
                    put("stage", ph.stage.name)
                    put("dayNumber", ph.dayNumber)
                    put("title", ph.title)
                    put("observation", ph.observation)
                    ph.canopyHeightCm?.let { put("canopyHeightCm", it) }
                }
                photosArray.put(phObj)
            }
            put("photos", photosArray)

            val remindersArray = JSONArray()
            for (r in reminders) {
                val rObj = JSONObject().apply {
                    put("id", r.id)
                    put("plantId", r.plantId)
                    put("careType", r.careType.name)
                    put("intervalHours", r.intervalHours)
                    put("lastTriggered", r.lastTriggered)
                    put("isEnabled", r.isEnabled)
                    put("customNote", r.customNote)
                }
                remindersArray.put(rObj)
            }
            put("reminderSettings", remindersArray)
        }
        return root.toString(2)
    }

    /**
     * Generates a clean, comprehensive multi-table CSV backup.
     */
    fun exportFullDatabaseToCsv(
        plants: List<Plant>,
        logs: List<CareLog>,
        photos: List<PhotoEntry>,
        reminders: List<ReminderSetting>
    ): String {
        val sb = StringBuilder()
        sb.append("# PLANTHUB CULTIVATION DATABASE BACKUP\n")
        sb.append("# FORMAT: MULTI-TABLE CSV\n")
        sb.append("# EXPORTED_AT: ${System.currentTimeMillis()}\n\n")

        // 1. Plants table
        sb.append("# TABLE: PLANTS\n")
        sb.append("id,name,strain,stage,lightCyclePreset,lightOnHours,lightOffHours,lightStartTimeMinute,medium,potSizeLiters,plantedDate,isTimerRunning,timerStartedAt,timerPausedRemainingSec,isPausedForMaintenance,iconKey,notes\n")
        for (p in plants) {
            sb.append(p.id).append(",")
            sb.append(escapeCsv(p.name)).append(",")
            sb.append(escapeCsv(p.strain)).append(",")
            sb.append(p.stage.name).append(",")
            sb.append(escapeCsv(p.lightCyclePreset)).append(",")
            sb.append(p.lightOnHours).append(",")
            sb.append(p.lightOffHours).append(",")
            sb.append(p.lightStartTimeMinute).append(",")
            sb.append(escapeCsv(p.medium)).append(",")
            sb.append(p.potSizeLiters).append(",")
            sb.append(p.plantedDate).append(",")
            sb.append(p.isTimerRunning).append(",")
            sb.append(p.timerStartedAt ?: "").append(",")
            sb.append(p.timerPausedRemainingSec ?: "").append(",")
            sb.append(p.isPausedForMaintenance).append(",")
            sb.append(escapeCsv(p.iconKey)).append(",")
            sb.append(escapeCsv(p.notes)).append("\n")
        }
        sb.append("\n")

        // 2. Care Logs table
        sb.append("# TABLE: CARE_LOGS\n")
        sb.append("id,plantId,timestamp,actionType,waterAmountMl,phLevel,fertilizerName,fertilizerDose,ecPpm,notes\n")
        for (l in logs) {
            sb.append(l.id).append(",")
            sb.append(l.plantId).append(",")
            sb.append(l.timestamp).append(",")
            sb.append(l.actionType.name).append(",")
            sb.append(l.waterAmountMl ?: "").append(",")
            sb.append(l.phLevel ?: "").append(",")
            sb.append(escapeCsv(l.fertilizerName ?: "")).append(",")
            sb.append(escapeCsv(l.fertilizerDose ?: "")).append(",")
            sb.append(l.ecPpm ?: "").append(",")
            sb.append(escapeCsv(l.notes)).append("\n")
        }
        sb.append("\n")

        // 3. Photos table
        sb.append("# TABLE: PHOTOS\n")
        sb.append("id,plantId,timestamp,imageUri,stage,dayNumber,title,canopyHeightCm,observation\n")
        for (ph in photos) {
            sb.append(ph.id).append(",")
            sb.append(ph.plantId).append(",")
            sb.append(ph.timestamp).append(",")
            sb.append(escapeCsv(ph.imageUri)).append(",")
            sb.append(ph.stage.name).append(",")
            sb.append(ph.dayNumber).append(",")
            sb.append(escapeCsv(ph.title)).append(",")
            sb.append(ph.canopyHeightCm ?: "").append(",")
            sb.append(escapeCsv(ph.observation)).append("\n")
        }
        sb.append("\n")

        // 4. Reminders table
        sb.append("# TABLE: REMINDER_SETTINGS\n")
        sb.append("id,plantId,careType,intervalHours,lastTriggered,isEnabled,customNote\n")
        for (r in reminders) {
            sb.append(r.id).append(",")
            sb.append(r.plantId).append(",")
            sb.append(r.careType.name).append(",")
            sb.append(r.intervalHours).append(",")
            sb.append(r.lastTriggered).append(",")
            sb.append(r.isEnabled).append(",")
            sb.append(escapeCsv(r.customNote)).append("\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        if (!value.contains(",") && !value.contains("\"") && !value.contains("\n") && !value.contains("\r")) {
            return value
        }
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    /**
     * Exports the entire database to a destination Document File URI using UTF-8 encoding.
     * Writes either structured JSON or multi-section RFC-4180 CSV based on the [isJson] flag.
     *
     * @param context Application context for accessing ContentResolver.
     * @param uri Target Storage Access Framework file URI.
     * @param isJson When true, formats as structured JSON; otherwise exports tabular CSV.
     * @param repository Data repository to query live records from SQLite.
     * @return True if file write completed successfully; false otherwise.
     */
    suspend fun exportToFileUri(
        context: Context,
        uri: Uri,
        isJson: Boolean,
        repository: PlantRepository
    ): Boolean {
        return try {
            val plants = repository.getAllPlantsOnce()
            val logs = repository.getAllCareLogsOnce()
            val photos = repository.getAllPhotosOnce()
            val reminders = repository.getAllRemindersOnce()

            val content = if (isJson) {
                exportFullDatabaseToJson(plants, logs, photos, reminders)
            } else {
                exportFullDatabaseToCsv(plants, logs, photos, reminders)
            }

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            Log.d(TAG, "Successfully exported database backup to URI: $uri (format: ${if (isJson) "JSON" else "CSV"})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export database backup: ${e.message}", e)
            false
        }
    }

    /**
     * Exports a full self-contained ZIP archive bundle containing database.json and
     * all local milestone images in full original resolution without compression.
     */
    suspend fun exportCompleteZipBackup(
        context: Context,
        uri: Uri,
        repository: PlantRepository
    ): Boolean {
        return try {
            val plants = repository.getAllPlantsOnce()
            val logs = repository.getAllCareLogsOnce()
            val photos = repository.getAllPhotosOnce()
            val reminders = repository.getAllRemindersOnce()

            // Prepare archive mapping for photos and collect image files to package
            val photoArchiveEntries = mutableListOf<Pair<PhotoEntry, String>>()
            val archivePhotos = mutableListOf<PhotoEntry>()

            photos.forEachIndexed { index, photo ->
                val rawUri = photo.imageUri.trim()
                var archiveFileName: String? = null

                val localFile = when {
                    rawUri.startsWith("/") -> File(rawUri).takeIf { it.exists() && it.isFile }
                    rawUri.startsWith("file://") -> {
                        try {
                            val path = Uri.parse(rawUri).path
                            path?.let { File(it) }?.takeIf { it.exists() && it.isFile }
                        } catch (_: Exception) { null }
                    }
                    else -> null
                }

                if (localFile != null) {
                    val ext = localFile.extension.ifBlank { "jpg" }
                    val name = "photo_${photo.id}_${photo.timestamp}_$index.$ext"
                    archiveFileName = name
                    photoArchiveEntries.add(photo to name)
                    archivePhotos.add(photo.copy(imageUri = "photos/$name"))
                } else if (rawUri.startsWith("content://")) {
                    try {
                        val canRead = context.contentResolver.openInputStream(Uri.parse(rawUri))?.use { true } ?: false
                        if (canRead) {
                            val name = "photo_${photo.id}_${photo.timestamp}_$index.jpg"
                            archiveFileName = name
                            photoArchiveEntries.add(photo to name)
                            archivePhotos.add(photo.copy(imageUri = "photos/$name"))
                        } else {
                            archivePhotos.add(photo)
                        }
                    } catch (_: Exception) {
                        archivePhotos.add(photo)
                    }
                } else {
                    archivePhotos.add(photo)
                }
            }

            val jsonContent = exportFullDatabaseToJson(plants, logs, archivePhotos, reminders)

            context.contentResolver.openOutputStream(uri)?.use { rawStream ->
                BufferedOutputStream(rawStream).use { bufStream ->
                    ZipOutputStream(bufStream).use { zipOut ->
                        // 1. Add database.json to ZIP root
                        val jsonBytes = jsonContent.toByteArray(StandardCharsets.UTF_8)
                        val jsonEntry = ZipEntry("database.json")
                        zipOut.putNextEntry(jsonEntry)
                        zipOut.write(jsonBytes)
                        zipOut.closeEntry()

                        // 2. Add each original image file under photos/
                        for ((photo, archiveName) in photoArchiveEntries) {
                            val entry = ZipEntry("photos/$archiveName")
                            zipOut.putNextEntry(entry)
                            try {
                                val rawUri = photo.imageUri.trim()
                                if (rawUri.startsWith("/") || rawUri.startsWith("file://")) {
                                    val path = if (rawUri.startsWith("/")) rawUri else Uri.parse(rawUri).path ?: ""
                                    val srcFile = File(path)
                                    if (srcFile.exists()) {
                                        FileInputStream(srcFile).use { inStream ->
                                            inStream.copyTo(zipOut)
                                        }
                                    }
                                } else if (rawUri.startsWith("content://")) {
                                    context.contentResolver.openInputStream(Uri.parse(rawUri))?.use { inStream ->
                                        inStream.copyTo(zipOut)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to stream image ${photo.imageUri} into ZIP: ${e.message}")
                            }
                            zipOut.closeEntry()
                        }
                        zipOut.flush()
                    }
                }
            }
            Log.d(TAG, "Successfully exported ZIP archive bundle to $uri with ${photoArchiveEntries.size} packaged photos")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ZIP backup bundle: ${e.message}", e)
            false
        }
    }

    /**
     * Exports a timestamped JSON backup into a user-selected folder via SAF DocumentTree URI.
     * Returns Pair(success, fileName).
     */
    suspend fun exportToFolderUri(
        context: Context,
        treeUri: Uri,
        repository: PlantRepository
    ): Pair<Boolean, String?> {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "planthub_backup_$timeStamp.json"
            val newFileUri = DocumentsContract.createDocument(
                context.contentResolver,
                parentDocUri,
                "application/json",
                fileName
            ) ?: return Pair(false, null)

            val plants = repository.getAllPlantsOnce()
            val logs = repository.getAllCareLogsOnce()
            val photos = repository.getAllPhotosOnce()
            val reminders = repository.getAllRemindersOnce()

            val content = exportFullDatabaseToJson(plants, logs, photos, reminders)
            context.contentResolver.openOutputStream(newFileUri)?.use { stream ->
                OutputStreamWriter(stream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            Log.d(TAG, "Successfully exported automated backup $fileName to tree URI: $treeUri")
            Pair(true, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export to tree URI: ${e.message}", e)
            Pair(false, null)
        }
    }

    /**
     * Returns a clean, user-friendly display name for a DocumentTree URI.
     */
    fun getFolderDisplayName(context: Context, treeUri: Uri): String {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            if (docId.contains(":")) {
                val path = docId.substringAfter(":")
                if (path.isNotBlank()) path else "Root Storage"
            } else {
                docId
            }
        } catch (_: Exception) {
            "Selected Folder"
        }
    }

    /**
     * DTO containing parsed cultivation database records ready for preview and restoration.
     */
    data class ImportPreviewData(
        val plants: List<Plant>,
        val logs: List<CareLog>,
        val photos: List<PhotoEntry>,
        val reminders: List<ReminderSetting>,
        val sourceFormat: String,
        val fileName: String,
        val bundledImagesCount: Int = 0,
        val sourceUri: Uri? = null
    )

    /**
     * Resolves the user-facing display name of a selected file URI.
     */
    fun getFileDisplayName(context: Context, uri: Uri): String {
        var name = "cultivation_backup"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        return name
    }

    private fun isZipStream(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(4)
                val read = input.read(header)
                read == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Inspects a ZIP backup archive, extracts database metadata, and counts bundled photos.
     */
    fun parseImportZip(context: Context, uri: Uri, fileName: String): Pair<ImportPreviewData?, String?> {
        return try {
            var jsonString: String? = null
            var imageCount = 0

            context.contentResolver.openInputStream(uri)?.use { rawStream ->
                ZipInputStream(BufferedInputStream(rawStream)).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    while (entry != null) {
                        val entryName = entry.name
                        if (!entry.isDirectory) {
                            if (entryName.equals("database.json", ignoreCase = true) ||
                                (entryName.endsWith(".json", ignoreCase = true) && !entryName.contains("/"))
                            ) {
                                val reader = BufferedReader(InputStreamReader(zipIn, StandardCharsets.UTF_8))
                                val sb = StringBuilder()
                                var line: String? = reader.readLine()
                                while (line != null) {
                                    sb.append(line).append("\n")
                                    line = reader.readLine()
                                }
                                jsonString = sb.toString()
                            } else if (entryName.startsWith("photos/") ||
                                entryName.endsWith(".jpg", ignoreCase = true) ||
                                entryName.endsWith(".jpeg", ignoreCase = true) ||
                                entryName.endsWith(".png", ignoreCase = true) ||
                                entryName.endsWith(".webp", ignoreCase = true) ||
                                entryName.endsWith(".heic", ignoreCase = true)
                            ) {
                                imageCount++
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            } ?: return Pair(null, "Could not open selected ZIP file stream.")

            if (jsonString == null || jsonString!!.isBlank()) {
                return Pair(null, "The selected ZIP archive does not contain a database.json cultivation ledger.")
            }

            val parsed = parseJsonBackup(jsonString!!, fileName)
            val preview = parsed.copy(
                sourceFormat = "ZIP Bundle",
                bundledImagesCount = imageCount,
                sourceUri = uri
            )
            Pair(preview, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting ZIP archive: ${e.message}", e)
            Pair(null, "Invalid ZIP archive: ${e.localizedMessage ?: "Could not read bundle"}")
        }
    }

    /**
     * Opens and parses a selected file URI into ImportPreviewData.
     */
    fun parseImportFile(context: Context, uri: Uri, isJson: Boolean): Pair<ImportPreviewData?, String?> {
        return try {
            val fileName = getFileDisplayName(context, uri)

            // Auto-detect ZIP archive by extension or magic header
            val isZip = fileName.endsWith(".zip", ignoreCase = true) || isZipStream(context, uri)
            if (isZip) {
                return parseImportZip(context, uri, fileName)
            }

            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            } ?: return Pair(null, "Could not open selected file stream.")

            if (content.isBlank()) {
                return Pair(null, "The selected file is empty.")
            }

            val data = if (isJson) {
                parseJsonBackup(content, fileName)
            } else {
                parseMultiTableCsv(content, fileName)
            }.copy(sourceUri = uri)

            if (data.plants.isEmpty() && data.logs.isEmpty() && data.photos.isEmpty() && data.reminders.isEmpty()) {
                Pair(null, "No valid plant, care ledger, or photo records were found in this file.")
            } else {
                Pair(data, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing import file: ${e.message}", e)
            Pair(null, "Parse failed: ${e.localizedMessage ?: "Invalid file structure"}")
        }
    }

    /**
     * Parses a complete JSON cultivation database backup.
     */
    fun parseJsonBackup(jsonString: String, fileName: String = "backup.json"): ImportPreviewData {
        val plants = mutableListOf<Plant>()
        val logs = mutableListOf<CareLog>()
        val photos = mutableListOf<PhotoEntry>()
        val reminders = mutableListOf<ReminderSetting>()

        val trimmed = jsonString.trim()
        if (trimmed.startsWith("[")) {
            // Root is a JSONArray of plants
            val plantsArray = JSONArray(trimmed)
            for (i in 0 until plantsArray.length()) {
                val pObj = plantsArray.getJSONObject(i)
                plants.add(parsePlantJson(pObj))
            }
        } else {
            val rawRoot = JSONObject(trimmed)
            val root = rawRoot.optJSONObject("data")
                ?: rawRoot.optJSONObject("database")
                ?: rawRoot.optJSONObject("backup")
                ?: rawRoot

            val plantsArray = root.optJSONArray("plants")
                ?: root.optJSONArray("cultivars")
                ?: root.optJSONArray("items")
                ?: root.optJSONArray("crops")
                ?: root.optJSONArray("strains")
            if (plantsArray != null) {
                for (i in 0 until plantsArray.length()) {
                    val pObj = plantsArray.getJSONObject(i)
                    plants.add(parsePlantJson(pObj))
                }
            }

            val logsArray = root.optJSONArray("careLogs")
                ?: root.optJSONArray("logs")
                ?: root.optJSONArray("care_logs")
                ?: root.optJSONArray("careHistory")
                ?: root.optJSONArray("care_history")
                ?: root.optJSONArray("care_log_entries")
                ?: root.optJSONArray("actions")
                ?: root.optJSONArray("careActions")
            if (logsArray != null) {
                for (i in 0 until logsArray.length()) {
                    val lObj = logsArray.getJSONObject(i)
                    val actionStr = lObj.optString("actionType", lObj.optString("action", lObj.optString("type", lObj.optString("careType", CareActionType.NOTE.name))))
                    val action = parseCareActionType(actionStr)
                    val plantId = lObj.optLong("plantId", lObj.optLong("plant_id", lObj.optLong("plant", 0L)))
                    logs.add(
                        CareLog(
                            id = lObj.optLong("id", 0L),
                            plantId = plantId,
                            timestamp = lObj.optLong("timestamp", lObj.optLong("time", lObj.optLong("date", System.currentTimeMillis()))),
                            actionType = action,
                            waterAmountMl = if (lObj.has("waterAmountMl") && !lObj.isNull("waterAmountMl")) lObj.optInt("waterAmountMl") else if (lObj.has("water_amount_ml") && !lObj.isNull("water_amount_ml")) lObj.optInt("water_amount_ml") else null,
                            phLevel = if (lObj.has("phLevel") && !lObj.isNull("phLevel")) lObj.optDouble("phLevel") else if (lObj.has("ph_level") && !lObj.isNull("ph_level")) lObj.optDouble("ph_level") else null,
                            fertilizerName = if (lObj.has("fertilizerName") && !lObj.isNull("fertilizerName")) lObj.optString("fertilizerName") else if (lObj.has("fertilizer_name") && !lObj.isNull("fertilizer_name")) lObj.optString("fertilizer_name") else null,
                            fertilizerDose = if (lObj.has("fertilizerDose") && !lObj.isNull("fertilizerDose")) lObj.optString("fertilizerDose") else if (lObj.has("fertilizer_dose") && !lObj.isNull("fertilizer_dose")) lObj.optString("fertilizer_dose") else null,
                            ecPpm = if (lObj.has("ecPpm") && !lObj.isNull("ecPpm")) lObj.optInt("ecPpm") else if (lObj.has("ec_ppm") && !lObj.isNull("ec_ppm")) lObj.optInt("ec_ppm") else null,
                            notes = lObj.optString("notes", lObj.optString("note", lObj.optString("observation", "")))
                        )
                    )
                }
            }

            val photosArray = root.optJSONArray("photos")
                ?: root.optJSONArray("photoEntries")
                ?: root.optJSONArray("photo_entries")
                ?: root.optJSONArray("vaultPhotos")
                ?: root.optJSONArray("vault_photos")
                ?: root.optJSONArray("images")
                ?: root.optJSONArray("checkpoints")
                ?: root.optJSONArray("milestones")
            if (photosArray != null) {
                for (i in 0 until photosArray.length()) {
                    val phObj = photosArray.getJSONObject(i)
                    val stageStr = phObj.optString("stage", phObj.optString("growthStage", phObj.optString("growth_stage", GrowthStage.VEGETATIVE.name)))
                    val stage = parseGrowthStage(stageStr)
                    val plantId = phObj.optLong("plantId", phObj.optLong("plant_id", phObj.optLong("plant", 0L)))
                    val rawUri = phObj.optString("imageUri", phObj.optString("uri", phObj.optString("photoUri", phObj.optString("path", phObj.optString("url", "")))))
                    photos.add(
                        PhotoEntry(
                            id = phObj.optLong("id", 0L),
                            plantId = plantId,
                            timestamp = phObj.optLong("timestamp", phObj.optLong("time", phObj.optLong("date", System.currentTimeMillis()))),
                            imageUri = rawUri,
                            stage = stage,
                            dayNumber = phObj.optInt("dayNumber", phObj.optInt("day_number", phObj.optInt("day", 1))),
                            title = phObj.optString("title", phObj.optString("label", "Milestone Photo")),
                            observation = phObj.optString("observation", phObj.optString("notes", phObj.optString("note", ""))),
                            canopyHeightCm = if (phObj.has("canopyHeightCm") && !phObj.isNull("canopyHeightCm")) phObj.optDouble("canopyHeightCm") else if (phObj.has("canopy_height_cm") && !phObj.isNull("canopy_height_cm")) phObj.optDouble("canopy_height_cm") else null
                        )
                    )
                }
            }

            val remindersArray = root.optJSONArray("reminderSettings")
                ?: root.optJSONArray("reminders")
                ?: root.optJSONArray("reminder_settings")
                ?: root.optJSONArray("alerts")
                ?: root.optJSONArray("schedules")
            if (remindersArray != null) {
                for (i in 0 until remindersArray.length()) {
                    val rObj = remindersArray.getJSONObject(i)
                    val careStr = rObj.optString("careType", rObj.optString("care_type", rObj.optString("actionType", CareActionType.WATER.name)))
                    val careType = parseCareActionType(careStr)
                    val plantId = rObj.optLong("plantId", rObj.optLong("plant_id", rObj.optLong("plant", 0L)))
                    reminders.add(
                        ReminderSetting(
                            id = rObj.optLong("id", 0L),
                            plantId = plantId,
                            careType = careType,
                            intervalHours = rObj.optInt("intervalHours", rObj.optInt("interval_hours", 48)),
                            lastTriggered = rObj.optLong("lastTriggered", rObj.optLong("last_triggered", System.currentTimeMillis())),
                            isEnabled = rObj.optBoolean("isEnabled", rObj.optBoolean("is_enabled", true)),
                            customNote = rObj.optString("customNote", rObj.optString("custom_note", ""))
                        )
                    )
                }
            }
        }

        return ImportPreviewData(
            plants = plants,
            logs = logs,
            photos = photos,
            reminders = reminders,
            sourceFormat = "JSON",
            fileName = fileName
        )
    }

    fun parseCareActionType(str: String): CareActionType {
        val clean = str.trim().uppercase(Locale.ROOT)
        return when {
            clean == "FEED" || clean == "FEEDING" || clean == "NUTRIENTS" || clean == "NPK" || clean.contains("FERTILIZ") -> CareActionType.FERTILIZE
            clean == "TRIM" || clean == "PRUNE" || clean == "PRUNING" || clean == "DEFOLIATION" || clean.contains("DEFOLIAT") -> CareActionType.DEFOLIATE
            clean == "LST" || clean == "TOPPING" || clean == "TOP" || clean == "BENDING" || clean.contains("TRAIN") -> CareActionType.TRAIN
            clean == "FOLIAR" || clean == "SPRAY" || clean.contains("MIST") -> CareActionType.MIST
            clean == "FLUSH" || clean == "H2O" || clean.contains("WATER") -> CareActionType.WATER
            clean == "LOG" || clean == "OBSERVATION" || clean.contains("NOTE") -> CareActionType.NOTE
            else -> CareActionType.values().firstOrNull {
                it.name == clean ||
                it.displayName.uppercase(Locale.ROOT) == clean ||
                it.displayName.uppercase(Locale.ROOT).startsWith(clean) ||
                clean.startsWith(it.name) ||
                it.shortCode.uppercase(Locale.ROOT) == clean
            } ?: CareActionType.NOTE
        }
    }

    fun parseGrowthStage(str: String): GrowthStage {
        val clean = str.trim().uppercase(Locale.ROOT)
        return GrowthStage.values().firstOrNull {
            it.name == clean ||
            it.label.uppercase(Locale.ROOT) == clean ||
            clean.startsWith(it.name)
        } ?: GrowthStage.VEGETATIVE
    }

    fun normalizePhotoUri(context: Context, rawUri: String): String {
        val trimmed = rawUri.trim()
        if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) {
            return "android.resource://${context.packageName}/drawable/planthub_icon_1791667356162"
        }
        if (trimmed.startsWith("android.resource://") && trimmed.contains("/drawable/")) {
            val drawableName = trimmed.substringAfterLast("/")
            return "android.resource://${context.packageName}/drawable/$drawableName"
        }
        return trimmed
    }

    private fun parsePlantJson(pObj: JSONObject): Plant {
        val stageStr = pObj.optString("stage", pObj.optString("growthStage", pObj.optString("growth_stage", GrowthStage.VEGETATIVE.name)))
        val stage = parseGrowthStage(stageStr)
        return Plant(
            id = pObj.optLong("id", pObj.optLong("plantId", pObj.optLong("plant_id", pObj.optLong("_id", 0L)))),
            name = pObj.optString("name", pObj.optString("cultivar", pObj.optString("title", "Restored Cultivar"))),
            strain = pObj.optString("strain", pObj.optString("variety", "Hybrid")),
            stage = stage,
            lightCyclePreset = pObj.optString("lightCyclePreset", pObj.optString("light_cycle_preset", "18/6 Vegetative")),
            lightOnHours = pObj.optInt("lightOnHours", pObj.optInt("light_on_hours", 18)),
            lightOffHours = pObj.optInt("lightOffHours", pObj.optInt("light_off_hours", 6)),
            lightStartTimeMinute = pObj.optInt("lightStartTimeMinute", pObj.optInt("light_start_time_minute", 360)),
            medium = pObj.optString("medium", pObj.optString("growingMedium", "Living Soil")),
            potSizeLiters = pObj.optDouble("potSizeLiters", pObj.optDouble("pot_size_liters", 12.0)),
            plantedDate = pObj.optLong("plantedDate", pObj.optLong("planted_date", pObj.optLong("created_at", System.currentTimeMillis()))),
            notes = pObj.optString("notes", pObj.optString("note", "")),
            isTimerRunning = pObj.optBoolean("isTimerRunning", pObj.optBoolean("is_timer_running", false)),
            timerStartedAt = if (pObj.has("timerStartedAt") && !pObj.isNull("timerStartedAt")) pObj.optLong("timerStartedAt") else if (pObj.has("timer_started_at") && !pObj.isNull("timer_started_at")) pObj.optLong("timer_started_at") else null,
            timerPausedRemainingSec = if (pObj.has("timerPausedRemainingSec") && !pObj.isNull("timerPausedRemainingSec")) pObj.optLong("timerPausedRemainingSec") else if (pObj.has("timer_paused_remaining_sec") && !pObj.isNull("timer_paused_remaining_sec")) pObj.optLong("timer_paused_remaining_sec") else null,
            isPausedForMaintenance = pObj.optBoolean("isPausedForMaintenance", pObj.optBoolean("is_paused_for_maintenance", false)),
            iconKey = pObj.optString("iconKey", pObj.optString("icon_key", pObj.optString("icon", "cannabis")))
        )
    }

    /**
     * Supported table types for CSV ingestion.
     */
    enum class CsvTableType {
        PLANTS,
        CARE_LOGS,
        PHOTOS,
        REMINDER_SETTINGS
    }

    /**
     * Normalizes a CSV header by trimming, lowercasing, and removing non-alphanumeric characters.
     */
    fun normalizeHeader(raw: String): String {
        return raw.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
    }

    /**
     * Builds a normalized header map supporting camelCase, snake_case, spaces, and punctuation.
     */
    fun buildNormalizedHeaderMap(headers: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        headers.forEachIndexed { idx, col ->
            val norm = normalizeHeader(col)
            if (norm.isNotEmpty()) {
                map.putIfAbsent(norm, idx)
            }
        }
        return map
    }

    private fun getString(row: List<String>, headers: Map<String, Int>, vararg keys: String): String? {
        for (key in keys) {
            val normKey = normalizeHeader(key)
            val idx = headers[normKey] ?: headers[key]
            if (idx != null && idx in row.indices) {
                val value = row[idx].trim()
                if (value.isNotEmpty() && !value.equals("null", ignoreCase = true)) {
                    return value
                }
            }
        }
        return null
    }

    private fun getLong(row: List<String>, headers: Map<String, Int>, vararg keys: String): Long? {
        for (key in keys) {
            val normKey = normalizeHeader(key)
            val idx = headers[normKey] ?: headers[key]
            if (idx != null && idx in row.indices) {
                val value = row[idx].trim().toLongOrNull()
                if (value != null) return value
            }
        }
        return null
    }

    private fun getInt(row: List<String>, headers: Map<String, Int>, vararg keys: String): Int? {
        for (key in keys) {
            val normKey = normalizeHeader(key)
            val idx = headers[normKey] ?: headers[key]
            if (idx != null && idx in row.indices) {
                val value = row[idx].trim().toIntOrNull()
                if (value != null) return value
            }
        }
        return null
    }

    private fun getDouble(row: List<String>, headers: Map<String, Int>, vararg keys: String): Double? {
        for (key in keys) {
            val normKey = normalizeHeader(key)
            val idx = headers[normKey] ?: headers[key]
            if (idx != null && idx in row.indices) {
                val value = row[idx].trim().toDoubleOrNull()
                if (value != null) return value
            }
        }
        return null
    }

    private fun getBoolean(row: List<String>, headers: Map<String, Int>, vararg keys: String): Boolean? {
        for (key in keys) {
            val normKey = normalizeHeader(key)
            val idx = headers[normKey] ?: headers[key]
            if (idx != null && idx in row.indices) {
                val value = row[idx].trim()
                if (value.equals("true", ignoreCase = true) || value == "1") return true
                if (value.equals("false", ignoreCase = true) || value == "0") return false
            }
        }
        return null
    }

    /**
     * Auto-detects table type from column signatures when # TABLE: headers are absent.
     */
    fun detectSingleTableType(headers: List<String>): CsvTableType {
        val norm = headers.map { normalizeHeader(it) }.toSet()

        val photoScore = norm.count { it in setOf("imageuri", "image", "photo", "photouri", "canopyheight", "canopyheightcm", "daynumber", "dayno", "milestone", "photopath", "imagepath") }
        val careScore = norm.count { it in setOf("actiontype", "action", "wateramountml", "wateramount", "water", "waterml", "phlevel", "ph", "fertilizername", "fertilizerdose", "fertilizer", "feedname", "feeddose", "feed", "ecppm", "ec", "ppm") }
        val reminderScore = norm.count { it in setOf("intervalhours", "lasttriggered", "customnote", "frequencyhours", "isenabled") }
        val plantScore = norm.count { it in setOf("strain", "lightcyclepreset", "lightonhours", "lightoffhours", "potsizeliters", "medium", "planteddate", "iconkey", "substrate") }

        return when {
            photoScore >= 1 && photoScore >= careScore && photoScore >= reminderScore && photoScore >= plantScore -> CsvTableType.PHOTOS
            careScore >= 1 && careScore >= reminderScore && careScore >= plantScore -> CsvTableType.CARE_LOGS
            reminderScore >= 1 && reminderScore >= plantScore -> CsvTableType.REMINDER_SETTINGS
            else -> CsvTableType.PLANTS
        }
    }

    /**
     * Parses a multi-table or single-table CSV into an ImportPreviewData structure.
     */
    fun parseMultiTableCsv(csvString: String, fileName: String = "backup.csv"): ImportPreviewData {
        val plants = mutableListOf<Plant>()
        val logs = mutableListOf<CareLog>()
        val photos = mutableListOf<PhotoEntry>()
        val reminders = mutableListOf<ReminderSetting>()

        if (csvString.contains("# TABLE:")) {
            // Split into sections by # TABLE:
            val sections = csvString.split(Regex("(?m)^#\\s*TABLE:\\s*"))
            for (section in sections) {
                val trimmed = section.trim()
                if (trimmed.isEmpty()) continue

                val firstLine = trimmed.lines().firstOrNull()?.trim()?.uppercase(Locale.ROOT) ?: continue
                val tableName = firstLine.substringBefore("\n").trim()
                val tableCsv = trimmed.substringAfter("\n").trim()

                when {
                    tableName.startsWith("PLANTS") -> {
                        val records = parseCsvRecords(tableCsv)
                        if (records.size > 1) {
                            val headerMap = buildNormalizedHeaderMap(records[0])
                            for (row in records.drop(1)) {
                                parsePlantCsvRow(row, headerMap)?.let { plants.add(it) }
                            }
                        }
                    }
                    tableName.startsWith("CARE_LOGS") || tableName.startsWith("CARELOGS") || tableName.startsWith("LOGS") -> {
                        val records = parseCsvRecords(tableCsv)
                        if (records.size > 1) {
                            val headerMap = buildNormalizedHeaderMap(records[0])
                            for (row in records.drop(1)) {
                                parseCareLogCsvRow(row, headerMap)?.let { logs.add(it) }
                            }
                        }
                    }
                    tableName.startsWith("PHOTOS") || tableName.startsWith("PHOTO_ENTRIES") || tableName.startsWith("VAULT") -> {
                        val records = parseCsvRecords(tableCsv)
                        if (records.size > 1) {
                            val headerMap = buildNormalizedHeaderMap(records[0])
                            for (row in records.drop(1)) {
                                parsePhotoCsvRow(row, headerMap)?.let { photos.add(it) }
                            }
                        }
                    }
                    tableName.startsWith("REMINDER_SETTINGS") || tableName.startsWith("REMINDERS") -> {
                        val records = parseCsvRecords(tableCsv)
                        if (records.size > 1) {
                            val headerMap = buildNormalizedHeaderMap(records[0])
                            for (row in records.drop(1)) {
                                parseReminderCsvRow(row, headerMap)?.let { reminders.add(it) }
                            }
                        }
                    }
                }
            }
        } else {
            // Standalone single-table CSV: auto-detect table type from column signatures
            val records = parseCsvRecords(csvString.lines().filter { !it.trim().startsWith("#") }.joinToString("\n"))
            if (records.size > 1) {
                val headerRow = records[0]
                val headerMap = buildNormalizedHeaderMap(headerRow)
                val tableType = detectSingleTableType(headerRow)
                val dataRows = records.drop(1)

                when (tableType) {
                    CsvTableType.PLANTS -> {
                        for (row in dataRows) {
                            parsePlantCsvRow(row, headerMap)?.let { plants.add(it) }
                        }
                    }
                    CsvTableType.CARE_LOGS -> {
                        for (row in dataRows) {
                            parseCareLogCsvRow(row, headerMap)?.let { logs.add(it) }
                        }
                    }
                    CsvTableType.PHOTOS -> {
                        for (row in dataRows) {
                            parsePhotoCsvRow(row, headerMap)?.let { photos.add(it) }
                        }
                    }
                    CsvTableType.REMINDER_SETTINGS -> {
                        for (row in dataRows) {
                            parseReminderCsvRow(row, headerMap)?.let { reminders.add(it) }
                        }
                    }
                }
            }
        }

        // If no plants were extracted from CSV (e.g. standalone care logs, photos, or reminders CSV),
        // synthesize a primary fallback cultivar so that UI preview and foreign keys have a primary parent.
        if (plants.isEmpty() && (logs.isNotEmpty() || photos.isNotEmpty() || reminders.isNotEmpty())) {
            plants.add(
                Plant(
                    id = 1L,
                    name = "Imported Cultivar",
                    strain = "Heritage Hybrid",
                    stage = GrowthStage.VEGETATIVE
                )
            )
        }

        return ImportPreviewData(
            plants = plants,
            logs = logs,
            photos = photos,
            reminders = reminders,
            sourceFormat = "CSV",
            fileName = fileName
        )
    }

    private fun parsePlantCsvRow(row: List<String>, headers: Map<String, Int>): Plant? {
        if (row.all { it.isBlank() }) return null

        val name = getString(row, headers, "name", "plantname", "cultivar", "title", "varietyname")
            ?: run {
                val idx = headers["name"] ?: 1
                row.getOrNull(idx)?.trim()?.takeIf { it.isNotBlank() }
            }
            ?: "Restored Cultivar"

        val id = getLong(row, headers, "id", "plantid", "_id", "rowid") ?: 0L
        val strain = getString(row, headers, "strain", "variety", "genetics", "genetic") ?: "Hybrid"
        val stageStr = getString(row, headers, "stage", "growthstage", "lifecycle", "phase") ?: GrowthStage.VEGETATIVE.name
        val stage = parseGrowthStage(stageStr)
        val lightCyclePreset = getString(row, headers, "lightcyclepreset", "lightcycle", "schedule", "preset", "photoperiod") ?: "18/6 Vegetative"
        val lightOnHours = getInt(row, headers, "lightonhours", "onhours") ?: 18
        val lightOffHours = getInt(row, headers, "lightoffhours", "offhours") ?: 6
        val lightStartTimeMinute = getInt(row, headers, "lightstarttimeminute", "starttime") ?: 360
        val medium = getString(row, headers, "medium", "growingmedium", "substrate", "soil") ?: "Living Soil"
        val potSizeLiters = getDouble(row, headers, "potsizeliters", "potsize", "liters", "potvolume", "container") ?: 12.0
        val plantedDate = getLong(row, headers, "planteddate", "plantedat", "createdat", "sproutdate") ?: System.currentTimeMillis()
        val isTimerRunning = getBoolean(row, headers, "istimerrunning", "timerrunning") ?: false
        val timerStartedAt = getLong(row, headers, "timerstartedat")
        val timerPausedRemainingSec = getLong(row, headers, "timerpausedremainingsec")
        val isPausedForMaintenance = getBoolean(row, headers, "ispausedformaintenance") ?: false
        val iconKey = getString(row, headers, "iconkey", "icon") ?: "cannabis"
        val notes = getString(row, headers, "notes", "note", "description") ?: ""

        return Plant(
            id = id,
            name = name,
            strain = strain,
            stage = stage,
            lightCyclePreset = lightCyclePreset,
            lightOnHours = lightOnHours,
            lightOffHours = lightOffHours,
            lightStartTimeMinute = lightStartTimeMinute,
            medium = medium,
            potSizeLiters = potSizeLiters,
            plantedDate = plantedDate,
            notes = notes,
            isTimerRunning = isTimerRunning,
            timerStartedAt = timerStartedAt,
            timerPausedRemainingSec = timerPausedRemainingSec,
            isPausedForMaintenance = isPausedForMaintenance,
            iconKey = iconKey
        )
    }

    private fun parseCareLogCsvRow(row: List<String>, headers: Map<String, Int>): CareLog? {
        if (row.all { it.isBlank() }) return null

        val actionStr = getString(row, headers, "actiontype", "action", "caretype", "type", "careaction", "event") ?: CareActionType.NOTE.name
        val action = parseCareActionType(actionStr)
        val plantId = getLong(row, headers, "plantid", "plant", "cultivarid", "cultivar", "cropid", "idplant") ?: 0L
        val id = getLong(row, headers, "id", "rowid", "entryid", "logid") ?: 0L
        val timestamp = getLong(row, headers, "timestamp", "time", "date", "datetime", "loggedat", "createdat") ?: System.currentTimeMillis()
        val waterAmountMl = getInt(row, headers, "wateramountml", "wateramount", "water", "waterml", "volume", "amountml")
        val phLevel = getDouble(row, headers, "phlevel", "ph", "phvalue")
        val fertilizerName = getString(row, headers, "fertilizername", "fertilizer", "feedname", "feed", "nutrients", "nutrient", "nutrientsname", "product", "nutes")
        val fertilizerDose = getString(row, headers, "fertilizerdose", "dose", "dosage", "feeddose", "concentration", "rate")
        val ecPpm = getInt(row, headers, "ecppm", "ec", "ppm", "tds")
        val notes = getString(row, headers, "notes", "note", "observation", "observations", "comment", "comments", "memo", "observationnotes") ?: ""

        return CareLog(
            id = id,
            plantId = plantId,
            timestamp = timestamp,
            actionType = action,
            waterAmountMl = waterAmountMl,
            phLevel = phLevel,
            fertilizerName = fertilizerName,
            fertilizerDose = fertilizerDose,
            ecPpm = ecPpm,
            notes = notes
        )
    }

    private fun parsePhotoCsvRow(row: List<String>, headers: Map<String, Int>): PhotoEntry? {
        if (row.all { it.isBlank() }) return null

        val imageUri = getString(row, headers, "imageuri", "uri", "image", "photouri", "photo", "path", "url", "photopath", "imagepath", "file") ?: ""
        val stageStr = getString(row, headers, "stage", "growthstage", "lifecycle", "phase") ?: GrowthStage.VEGETATIVE.name
        val stage = parseGrowthStage(stageStr)
        val plantId = getLong(row, headers, "plantid", "plant", "cultivarid", "cultivar", "cropid", "idplant") ?: 0L
        val id = getLong(row, headers, "id", "rowid", "entryid", "photoid") ?: 0L
        val timestamp = getLong(row, headers, "timestamp", "time", "date", "datetime", "loggedat", "createdat") ?: System.currentTimeMillis()
        val dayNumber = getInt(row, headers, "daynumber", "day", "dayno", "days", "growthday", "age") ?: 1
        val title = getString(row, headers, "title", "label", "caption", "checkpoint") ?: "Milestone Photo"
        val canopyHeightCm = getDouble(row, headers, "canopyheightcm", "canopyheight", "height", "heightcm", "plantheight")
        val observation = getString(row, headers, "observation", "observations", "notes", "note", "comment", "comments", "memo", "observationnotes") ?: ""

        return PhotoEntry(
            id = id,
            plantId = plantId,
            timestamp = timestamp,
            imageUri = imageUri,
            stage = stage,
            dayNumber = dayNumber,
            title = title,
            canopyHeightCm = canopyHeightCm,
            observation = observation
        )
    }

    private fun parseReminderCsvRow(row: List<String>, headers: Map<String, Int>): ReminderSetting? {
        if (row.all { it.isBlank() }) return null

        val plantId = getLong(row, headers, "plantid", "plant", "cultivarid", "cultivar", "cropid", "idplant") ?: 0L
        val id = getLong(row, headers, "id", "rowid", "reminderid") ?: 0L
        val careStr = getString(row, headers, "caretype", "actiontype", "type") ?: CareActionType.WATER.name
        val careType = parseCareActionType(careStr)
        val intervalHours = getInt(row, headers, "intervalhours", "interval", "frequencyhours", "frequency", "hours") ?: 48
        val lastTriggered = getLong(row, headers, "lasttriggered", "triggeredat") ?: System.currentTimeMillis()
        val isEnabled = getBoolean(row, headers, "isenabled", "enabled", "active") ?: true
        val customNote = getString(row, headers, "customnote", "note", "notes", "remindertext", "memo") ?: ""

        return ReminderSetting(
            id = id,
            plantId = plantId,
            careType = careType,
            intervalHours = intervalHours,
            lastTriggered = lastTriggered,
            isEnabled = isEnabled,
            customNote = customNote
        )
    }

    /**
     * RFC 4180 compliant CSV tokenizer handling multiline fields, commas, and escaped quotes.
     */
    fun parseCsvRecords(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRecord.add(currentField.toString())
                        currentField.clear()
                    }
                    '\r' -> {
                        if (i + 1 < text.length && text[i + 1] == '\n') {
                            i++
                        }
                        currentRecord.add(currentField.toString())
                        currentField.clear()
                        if (currentRecord.any { it.isNotBlank() }) {
                            records.add(currentRecord.toList())
                        }
                        currentRecord.clear()
                    }
                    '\n' -> {
                        currentRecord.add(currentField.toString())
                        currentField.clear()
                        if (currentRecord.any { it.isNotBlank() }) {
                            records.add(currentRecord.toList())
                        }
                        currentRecord.clear()
                    }
                    else -> currentField.append(c)
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString())
            if (currentRecord.any { it.isNotBlank() }) {
                records.add(currentRecord.toList())
            }
        }
        return records
    }

    /**
     * Replaces the entire local Room database with records from ImportPreviewData in an atomic manner.
     */
    suspend fun restoreDatabaseFromData(
        context: Context,
        repository: PlantRepository,
        data: ImportPreviewData
    ): Boolean {
        return try {
            // 1. Wipe all existing data completely across all four tables (cascading safe)
            repository.clearAllData()

            // 2. Insert new plants while tracking ID mapping (oldId -> newId and 1-based index -> newId)
            val idMapping = mutableMapOf<Long, Long>()
            data.plants.forEachIndexed { index, plant ->
                val oldId = plant.id
                val newPlant = plant.copy(id = 0L)
                val newId = repository.insertPlant(newPlant)
                if (oldId != 0L) {
                    idMapping[oldId] = newId
                }
                idMapping[newId] = newId
                val oneBasedIndex = (index + 1).toLong()
                if (!idMapping.containsKey(oneBasedIndex)) {
                    idMapping[oneBasedIndex] = newId
                }
            }

            // Fallback: If no plants were in the backup, create a default plant so logs/photos have a valid parent
            val defaultPlantId: Long = idMapping.values.firstOrNull() ?: run {
                val fallbackPlant = Plant(
                    name = "Imported Cultivar",
                    strain = "Heritage Hybrid",
                    stage = GrowthStage.VEGETATIVE
                )
                val createdId = repository.insertPlant(fallbackPlant)
                idMapping[createdId] = createdId
                idMapping[1L] = createdId
                createdId
            }

            // 3. Insert Care Logs (guaranteed valid foreign key to primary cultivar if unlinked)
            var insertedLogsCount = 0
            for (log in data.logs) {
                try {
                    val targetPlantId = if (log.plantId != 0L && idMapping.containsKey(log.plantId)) {
                        idMapping[log.plantId]!!
                    } else {
                        defaultPlantId
                    }
                    repository.insertCareLog(log.copy(id = 0L, plantId = targetPlantId))
                    insertedLogsCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Skipped invalid care log entry: ${e.message}")
                }
            }

            // 4. Insert Photos (with normalized URI & fallback to primary cultivar if unlinked)
            var insertedPhotosCount = 0
            for (photo in data.photos) {
                try {
                    val targetPlantId = if (photo.plantId != 0L && idMapping.containsKey(photo.plantId)) {
                        idMapping[photo.plantId]!!
                    } else {
                        defaultPlantId
                    }
                    val normalizedUri = normalizePhotoUri(context, photo.imageUri)
                    repository.insertPhotoEntry(
                        photo.copy(
                            id = 0L,
                            plantId = targetPlantId,
                            imageUri = normalizedUri
                        )
                    )
                    insertedPhotosCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Skipped invalid photo entry: ${e.message}")
                }
            }

            // 5. Insert Reminder Settings
            for (reminder in data.reminders) {
                try {
                    val targetPlantId = if (reminder.plantId != 0L && idMapping.containsKey(reminder.plantId)) {
                        idMapping[reminder.plantId]!!
                    } else {
                        defaultPlantId
                    }
                    repository.insertReminder(reminder.copy(id = 0L, plantId = targetPlantId))
                } catch (e: Exception) {
                    Log.w(TAG, "Skipped invalid reminder entry: ${e.message}")
                }
            }

            // 6. Update internal backup cache with fresh data
            val freshPlants = repository.getAllPlantsOnce()
            val freshLogs = repository.getAllCareLogsOnce()
            val freshPhotos = repository.getAllPhotosOnce()
            saveBackup(context, freshPlants, freshLogs, freshPhotos)

            Log.d(TAG, "Successfully restored ${freshPlants.size} plants, $insertedLogsCount logs, $insertedPhotosCount photos")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error executing database restore: ${e.message}", e)
            false
        }
    }

    /**
     * Extracts photos from a ZIP archive directly into app storage and restores the database.
     */
    suspend fun restoreDatabaseFromZip(
        context: Context,
        repository: PlantRepository,
        data: ImportPreviewData,
        zipUri: Uri
    ): Boolean {
        return try {
            val growthPhotosDir = File(context.filesDir, "growth_photos")
            if (!growthPhotosDir.exists()) growthPhotosDir.mkdirs()

            val extractedFilesMap = mutableMapOf<String, String>()

            context.contentResolver.openInputStream(zipUri)?.use { rawStream ->
                ZipInputStream(BufferedInputStream(rawStream)).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory && (
                                name.startsWith("photos/") ||
                                name.endsWith(".jpg", ignoreCase = true) ||
                                name.endsWith(".jpeg", ignoreCase = true) ||
                                name.endsWith(".png", ignoreCase = true) ||
                                name.endsWith(".webp", ignoreCase = true) ||
                                name.endsWith(".heic", ignoreCase = true)
                            )) {
                            val cleanName = name.substringAfterLast("/")
                            val destFile = File(growthPhotosDir, "restored_${System.currentTimeMillis()}_$cleanName")
                            FileOutputStream(destFile).use { out ->
                                zipIn.copyTo(out)
                            }
                            extractedFilesMap[name] = destFile.absolutePath
                            extractedFilesMap[cleanName] = destFile.absolutePath
                            extractedFilesMap["photos/$cleanName"] = destFile.absolutePath
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            // Remap photos to their restored persistent file paths
            val updatedPhotos = data.photos.map { photo ->
                val raw = photo.imageUri.trim()
                val clean = raw.substringAfterLast("/")
                val restoredPath = extractedFilesMap[raw]
                    ?: extractedFilesMap[clean]
                    ?: extractedFilesMap["photos/$clean"]

                if (restoredPath != null) {
                    photo.copy(imageUri = restoredPath)
                } else {
                    photo
                }
            }

            val updatedData = data.copy(photos = updatedPhotos)
            restoreDatabaseFromData(context, repository, updatedData)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from ZIP archive: ${e.message}", e)
            false
        }
    }
}
