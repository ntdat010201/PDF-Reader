package com.example.pdfreader.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class PdfFile(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val size: Long = file.length(),
    val lastModified: Long = file.lastModified()
) {
    
    /**
     * Lấy kích thước file dưới dạng string có định dạng (KB, MB, GB)
     */
    fun getFormattedSize(): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        
        return when {
            size >= gb -> String.format("%.1f GB", size / gb)
            size >= mb -> String.format("%.1f MB", size / mb)
            size >= kb -> String.format("%.1f KB", size / kb)
            else -> "$size B"
        }
    }
    
    /**
     * Lấy ngày sửa đổi cuối cùng dưới dạng string có định dạng
     */
    fun getFormattedDate(): String {
        val date = Date(lastModified)
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Lấy tên file không có extension
     */
    fun getNameWithoutExtension(): String {
        return name.substringBeforeLast(".")
    }
    
    /**
     * Kiểm tra xem file có tồn tại và có thể đọc được không
     */
    fun isValid(): Boolean {
        return file.exists() && file.canRead() && file.isFile
    }
}