package com.example.pdfreader.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.pdfreader.model.PdfFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class FileManager(private val context: Context) {

    companion object {
        private const val TAG = "FileManager"
    }

    /**
     * Lấy danh sách tất cả file PDF từ bộ nhớ
     */
    fun getAllPdfFiles(): List<PdfFile> {
        val pdfFiles = mutableListOf<PdfFile>()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ với MANAGE_EXTERNAL_STORAGE
                if (Environment.isExternalStorageManager()) {
                    pdfFiles.addAll(getPdfFilesFromExternalStorage())
                } else {
                    Log.w(TAG, "MANAGE_EXTERNAL_STORAGE permission not granted")
                }
            } else {
                // Android 10 và thấp hơn với READ_EXTERNAL_STORAGE
                pdfFiles.addAll(getPdfFilesFromMediaStore())
                pdfFiles.addAll(getPdfFilesFromExternalStorage())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting PDF files", e)
        }

        return pdfFiles.distinctBy { it.path }
    }

    /**
     * Lấy file PDF từ MediaStore (phù hợp cho Android 10 trở xuống)
     */
    private fun getPdfFilesFromMediaStore(): List<PdfFile> {
        val pdfFiles = mutableListOf<PdfFile>()
        
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/pdf")

        try {
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val path = cursor.getString(pathColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateColumn) * 1000 // Convert to milliseconds

                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        pdfFiles.add(PdfFile(file))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore", e)
        }

        return pdfFiles
    }

    /**
     * Lấy file PDF từ External Storage (phù hợp cho Android 11+ với MANAGE_EXTERNAL_STORAGE)
     */
    private fun getPdfFilesFromExternalStorage(): List<PdfFile> {
        val pdfFiles = mutableListOf<PdfFile>()
        
        try {
            val externalStorageDir = Environment.getExternalStorageDirectory()
            if (externalStorageDir.exists() && externalStorageDir.canRead()) {
                scanDirectoryForPdfFiles(externalStorageDir, pdfFiles)
            }

            // Scan additional common directories
            val commonDirs = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                File(Environment.getExternalStorageDirectory(), "Documents"),
                File(Environment.getExternalStorageDirectory(), "Download")
            )

            commonDirs.forEach { dir ->
                if (dir.exists() && dir.canRead()) {
                    scanDirectoryForPdfFiles(dir, pdfFiles)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning external storage", e)
        }

        return pdfFiles
    }

    /**
     * Quét thư mục để tìm file PDF
     */
    private fun scanDirectoryForPdfFiles(directory: File, pdfFiles: MutableList<PdfFile>) {
        try {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isDirectory && file.canRead() -> {
                        // Recursively scan subdirectories
                        scanDirectoryForPdfFiles(file, pdfFiles)
                    }
                    file.isFile && file.extension.lowercase() == "pdf" && file.canRead() -> {
                        pdfFiles.add(PdfFile(file))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory: ${directory.absolutePath}", e)
        }
    }

    /**
     * Đổi tên file PDF
     */
    fun renameFile(pdfFile: PdfFile, newName: String): Boolean {
        return try {
            val originalFile = pdfFile.file
            if (!originalFile.exists()) {
                Log.e(TAG, "Original file does not exist: ${pdfFile.path}")
                return false
            }

            val newFileName = if (newName.endsWith(".pdf", ignoreCase = true)) {
                newName
            } else {
                "$newName.pdf"
            }

            val newFile = File(originalFile.parent, newFileName)
            
            if (newFile.exists()) {
                Log.e(TAG, "File with new name already exists: ${newFile.absolutePath}")
                return false
            }

            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                // Android 11+ với MANAGE_EXTERNAL_STORAGE
                originalFile.renameTo(newFile)
            } else {
                // Android 10 trở xuống hoặc không có MANAGE_EXTERNAL_STORAGE
                copyAndDeleteFile(originalFile, newFile)
            }

            if (success) {
                Log.i(TAG, "File renamed successfully: ${originalFile.absolutePath} -> ${newFile.absolutePath}")
            } else {
                Log.e(TAG, "Failed to rename file: ${originalFile.absolutePath}")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error renaming file: ${pdfFile.path}", e)
            false
        }
    }

    /**
     * Xóa file PDF
     */
    fun deleteFile(pdfFile: PdfFile): Boolean {
        return try {
            val file = pdfFile.file
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: ${pdfFile.path}")
                return false
            }

            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                // Android 11+ với MANAGE_EXTERNAL_STORAGE
                file.delete()
            } else {
                // Android 10 trở xuống
                file.delete()
            }

            if (success) {
                Log.i(TAG, "File deleted successfully: ${pdfFile.path}")
            } else {
                Log.e(TAG, "Failed to delete file: ${pdfFile.path}")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: ${pdfFile.path}", e)
            false
        }
    }

    /**
     * Copy file và xóa file gốc (fallback cho rename khi không có quyền direct rename)
     */
    private fun copyAndDeleteFile(sourceFile: File, destFile: File): Boolean {
        return try {
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Verify copy was successful
            if (destFile.exists() && destFile.length() == sourceFile.length()) {
                sourceFile.delete()
            } else {
                // Clean up failed copy
                if (destFile.exists()) {
                    destFile.delete()
                }
                false
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error copying file", e)
            // Clean up partial copy
            if (destFile.exists()) {
                destFile.delete()
            }
            false
        }
    }

    /**
     * Kiểm tra xem file có tồn tại và có thể đọc được không
     */
    fun isFileAccessible(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.exists() && file.canRead()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking file accessibility: $filePath", e)
            false
        }
    }

    /**
     * Lấy thông tin chi tiết của file
     */
    fun getFileInfo(filePath: String): PdfFile? {
        return try {
            val file = File(filePath)
            if (file.exists() && file.canRead()) {
                PdfFile(file)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file info: $filePath", e)
            null
        }
    }
}