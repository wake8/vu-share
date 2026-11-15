package com.vunnam.storage

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.vunnam.model.FileCategory
import com.vunnam.model.SelectableFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class StorageManager(private val context: Context) {

    /**
     * Resolves the primary directory where received files will be stored.
     */
    fun getDownloadDirectory(): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val vuShareDir = File(publicDownloads, "VUShare")
        if (!vuShareDir.exists()) {
            vuShareDir.mkdirs()
        }
        if (vuShareDir.canWrite()) {
            return vuShareDir
        }
        // Fallback to app external files dir if public downloads is restricted
        val appDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val appVuShareDir = File(appDownloads, "VUShare")
        if (!appVuShareDir.exists()) {
            appVuShareDir.mkdirs()
        }
        return appVuShareDir
    }

    fun getAvailableBytes(): Long {
        return try {
            val dir = getDownloadDirectory()
            StatFs(dir.absolutePath).availableBytes
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }

    /**
     * Queries MediaStore for photos.
     */
    suspend fun queryPhotos(): List<SelectableFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<SelectableFile>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        try {
            context.contentResolver.query(queryUri, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "photo_$id.jpg"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val date = cursor.getLong(dateCol) * 1000L
                    val contentUri = ContentUris.withAppendedId(queryUri, id)

                    if (size > 0) {
                        files.add(
                            SelectableFile(
                                uri = contentUri,
                                name = name,
                                size = size,
                                mimeType = mime,
                                category = FileCategory.PHOTOS,
                                dateModified = date
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        files
    }

    /**
     * Queries MediaStore for videos.
     */
    suspend fun queryVideos(): List<SelectableFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<SelectableFile>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        val queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        try {
            context.contentResolver.query(queryUri, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "video_$id.mp4"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "video/mp4"
                    val date = cursor.getLong(dateCol) * 1000L
                    val contentUri = ContentUris.withAppendedId(queryUri, id)

                    if (size > 0) {
                        files.add(
                            SelectableFile(
                                uri = contentUri,
                                name = name,
                                size = size,
                                mimeType = mime,
                                category = FileCategory.VIDEOS,
                                dateModified = date
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        files
    }

    /**
     * Queries MediaStore for music/audio files.
     */
    suspend fun queryMusic(): List<SelectableFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<SelectableFile>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
        val queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        try {
            context.contentResolver.query(queryUri, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "audio_$id.mp3"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "audio/mpeg"
                    val date = cursor.getLong(dateCol) * 1000L
                    val contentUri = ContentUris.withAppendedId(queryUri, id)

                    if (size > 0) {
                        files.add(
                            SelectableFile(
                                uri = contentUri,
                                name = name,
                                size = size,
                                mimeType = mime,
                                category = FileCategory.MUSIC,
                                dateModified = date
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        files
    }

    /**
     * Queries MediaStore for common documents (PDF, DOC, TXT, XLS, PPT, ZIP).
     */
    suspend fun queryDocuments(): List<SelectableFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<SelectableFile>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        val queryUri = MediaStore.Files.getContentUri("external")
        val mimeTypes = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "application/zip"
        )
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} IN (${mimeTypes.joinToString(",") { "?" }})"

        try {
            context.contentResolver.query(queryUri, projection, selection, mimeTypes, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "document_$id"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "application/octet-stream"
                    val date = cursor.getLong(dateCol) * 1000L
                    val contentUri = ContentUris.withAppendedId(queryUri, id)

                    if (size > 0) {
                        files.add(
                            SelectableFile(
                                uri = contentUri,
                                name = name,
                                size = size,
                                mimeType = mime,
                                category = FileCategory.DOCUMENTS,
                                dateModified = date
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        files
    }

    /**
     * Queries installed applications to share their APK files.
     */
    suspend fun queryInstalledApps(): List<SelectableFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<SelectableFile>()
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in installedApps) {
            // Filter out internal system apps that don't have launch intents, or include user apps
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val isUserApp = !isSystem || isUpdatedSystem

            val apkFile = File(appInfo.sourceDir)
            if (apkFile.exists() && apkFile.canRead() && (isUserApp || pm.getLaunchIntentForPackage(appInfo.packageName) != null)) {
                val appLabel = pm.getApplicationLabel(appInfo).toString()
                val apkName = "${FileSecuritySanitizer.sanitizeFilename(appLabel)}.apk"
                val apkUri = Uri.fromFile(apkFile)

                files.add(
                    SelectableFile(
                        uri = apkUri,
                        name = apkName,
                        size = apkFile.length(),
                        mimeType = "application/vnd.android.package-archive",
                        category = FileCategory.APPS,
                        dateModified = apkFile.lastModified(),
                        appPackageName = appInfo.packageName
                    )
                )
            }
        }
        files.sortedBy { it.name.lowercase() }
    }

    /**
     * Resolves metadata from a user-picked SAF Uri.
     */
    fun resolveUriMetadata(uri: Uri): SelectableFile? {
        try {
            var displayName = "file_${System.currentTimeMillis()}"
            var size = 0L
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }

            // Fallback for file:// or missing size
            if (size <= 0) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        size = stream.available().toLong()
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }

            val category = when {
                mimeType.startsWith("image/") -> FileCategory.PHOTOS
                mimeType.startsWith("video/") -> FileCategory.VIDEOS
                mimeType.startsWith("audio/") -> FileCategory.MUSIC
                mimeType == "application/vnd.android.package-archive" -> FileCategory.APPS
                mimeType.contains("pdf") || mimeType.contains("document") || mimeType.contains("text") -> FileCategory.DOCUMENTS
                else -> FileCategory.FILES
            }

            return SelectableFile(
                uri = uri,
                name = displayName,
                size = size,
                mimeType = mimeType,
                category = category
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Launches an intent to view or open a received file.
     */
    fun openFile(localFilePath: String): Boolean {
        val file = File(localFilePath)
        if (!file.exists()) return false

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // If specific viewer isn't available, try generic open
            try {
                val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    /**
     * Launches an intent to re-share a file.
     */
    fun shareFile(localFilePath: String) {
        val file = File(localFilePath)
        if (!file.exists()) return

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share file via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
