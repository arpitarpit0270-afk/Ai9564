package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.Locale

data class CreatedDeviceInfo(
    val success: Boolean = true,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long = 0L,
    val lineCount: Int = 0,
    val extension: String = "txt",
    val uriString: String? = null,
    val uri: Uri? = null,
    val file: File? = null,
    val folderType: String = "Downloads",
    val mimeType: String = "text/plain",
    val errorMessage: String? = null
) {
    val fileSizeKB: String
        get() = String.format(Locale.US, "%.1f", (fileSizeBytes / 1024f).coerceAtLeast(0.1f))
}

data class UploadedFileInfo(
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val textContent: String? = null,
    val isImage: Boolean = false,
    val base64Data: String? = null,
    val uri: Uri? = null
) {
    val sizeFormatted: String
        get() = if (sizeBytes > 1024 * 1024) {
            String.format(Locale.US, "%.1f MB", sizeBytes / (1024f * 1024f))
        } else {
            "${(sizeBytes / 1024).coerceAtLeast(1)} KB"
        }
}

object JarvisDeviceFileManager {

    /**
     * Converts a Bitmap image into Base64 JPEG string for Vision AI.
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Creates and saves a file with the given code/text content directly on the device storage.
     */
    fun createDeviceFile(context: Context, rawFileName: String, content: String): CreatedDeviceInfo {
        return try {
            val cleanName = rawFileName.trim().replace(Regex("[/\\\\:;*?\"<>|]"), "_").ifBlank { "jarvis_generated_${System.currentTimeMillis()}.txt" }
            
            // Prefer Downloads folder if accessible, otherwise Documents or App external directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var folderType = "Downloads/JarvisAI"
            val targetDir = File(downloadsDir, "JarvisAI").apply {
                if (!exists()) mkdirs()
            }

            val finalFile = if (targetDir.exists() && targetDir.canWrite()) {
                File(targetDir, cleanName)
            } else {
                folderType = "AppStorage/JarvisAI"
                val fallbackDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "JarvisAI").apply {
                    if (!exists()) mkdirs()
                }
                File(fallbackDir, cleanName)
            }

            FileOutputStream(finalFile).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            val lineCount = content.lines().size
            val extension = cleanName.substringAfterLast('.', "txt").lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "text/plain"

            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", finalFile)
            } catch (_: Exception) {
                Uri.fromFile(finalFile)
            }

            CreatedDeviceInfo(
                success = true,
                fileName = finalFile.name,
                filePath = finalFile.absolutePath,
                fileSizeBytes = finalFile.length(),
                lineCount = lineCount,
                extension = extension,
                uriString = uri.toString(),
                uri = uri,
                file = finalFile,
                folderType = folderType,
                mimeType = mime,
                errorMessage = null
            )
        } catch (e: Exception) {
            CreatedDeviceInfo(
                success = false,
                fileName = rawFileName,
                filePath = "",
                errorMessage = e.localizedMessage ?: "Unknown file creation error"
            )
        }
    }

    /**
     * Reads text/code content from a picked File Uri.
     */
    fun readFileContent(context: Context, uri: Uri): UploadedFileInfo {
        val contentResolver = context.contentResolver
        var fileName = "unknown_file"
        var fileSize = 0L
        val mimeType = contentResolver.getType(uri) ?: "text/plain"

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: "file"
                    if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {}

        val isImage = mimeType.startsWith("image/") || fileName.endsWith(".jpg", true) || fileName.endsWith(".png", true) || fileName.endsWith(".jpeg", true) || fileName.endsWith(".webp", true)

        if (isImage) {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            return UploadedFileInfo(
                name = fileName,
                sizeBytes = if (fileSize > 0) fileSize else bytes.size.toLong(),
                mimeType = mimeType,
                textContent = "[Image File: $fileName]",
                isImage = true,
                base64Data = base64,
                uri = uri
            )
        }

        // Text / Code / Document file reading
        val textBuilder = StringBuilder()
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    var linesRead = 0
                    while (reader.readLine().also { line = it } != null && linesRead < 3000) {
                        textBuilder.append(line).append("\n")
                        linesRead++
                    }
                }
            }
        } catch (e: Exception) {
            textBuilder.append("Error reading file content: ${e.localizedMessage}")
        }

        val text = textBuilder.toString().trim()
        return UploadedFileInfo(
            name = fileName,
            sizeBytes = if (fileSize > 0) fileSize else text.toByteArray().size.toLong(),
            mimeType = mimeType,
            textContent = text,
            isImage = false,
            uri = uri
        )
    }

    /**
     * Opens a created file in system code editor / viewer
     */
    fun openFile(context: Context, file: File? = null, uri: Uri? = null, mimeType: String? = null): ActionResult {
        return try {
            val resolvedUri = uri ?: file?.let {
                try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
                } catch (_: Exception) {
                    Uri.fromFile(it)
                }
            } ?: return ActionResult(false, "No valid file target to open.")

            val ext = file?.extension?.lowercase() ?: "txt"
            val resolvedMime = mimeType ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "text/plain"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(resolvedUri, resolvedMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open file with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            val targetName = file?.name ?: "file"
            ActionResult(true, "Opening $targetName in viewer, sir.")
        } catch (e: Exception) {
            ActionResult(false, "Could not open file: ${e.localizedMessage}")
        }
    }

    /**
     * Shares a created file via WhatsApp, Telegram, Email, etc.
     */
    fun shareFile(context: Context, file: File? = null, uri: Uri? = null, mimeType: String? = null): ActionResult {
        return try {
            val resolvedUri = uri ?: file?.let {
                try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
                } catch (_: Exception) {
                    Uri.fromFile(it)
                }
            } ?: return ActionResult(false, "No valid file target to share.")

            val ext = file?.extension?.lowercase() ?: "txt"
            val resolvedMime = mimeType ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
            val targetName = file?.name ?: "file"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = resolvedMime
                putExtra(Intent.EXTRA_STREAM, resolvedUri)
                putExtra(Intent.EXTRA_SUBJECT, "Created by J.A.R.V.I.S. AI: $targetName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share $targetName via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            ActionResult(true, "Preparing to share $targetName, sir.")
        } catch (e: Exception) {
            ActionResult(false, "Could not share file: ${e.localizedMessage}")
        }
    }
}
