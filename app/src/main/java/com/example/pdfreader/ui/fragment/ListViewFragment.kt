package com.example.pdfreader.ui.fragment

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pdfreader.ui.decoration.GridSpacingItemDecoration
import com.example.pdfreader.adapter.ListViewAdapter
import com.example.pdfreader.databinding.FragmentListViewBinding
import com.example.pdfreader.model.PdfFile
import com.example.pdfreader.ui.activities.PdfViewerActivity
import com.example.pdfreader.dialog.FileOptionsBottomSheetDialog
import com.example.pdfreader.dialog.SortOptionsBottomSheetDialog
import kotlinx.coroutines.*
import java.io.File

class ListViewFragment : Fragment() {
    
    enum class SortType(val displayName: String) {
        NAME_A_TO_Z("Name A-Z"),
        NAME_Z_TO_A("Name Z-A"),
        TIME_OLD_TO_NEW("Time: Old → New"),
        TIME_NEW_TO_OLD("Time: New → Old"),
        SIZE_SMALL_TO_LARGE("Capacity: Small → Large"),
        SIZE_LARGE_TO_SMALL("Capacity: Large → Small")
    }
    
    enum class LayoutType {
        LINEAR,
        GRID
    }
    
    private lateinit var binding: FragmentListViewBinding
    private lateinit var adapter: ListViewAdapter
    private var allPdfFiles = mutableListOf<PdfFile>()
    private var currentSortType = SortType.NAME_A_TO_Z
    private var currentLayoutType = LayoutType.LINEAR
    private var gridItemDecoration: GridSpacingItemDecoration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListViewBinding.inflate(inflater, container, false)
        initData()
        return binding.root
    }

    private fun initData() {
        setupRecyclerView()
        setupSearchView()
        setupSortButton()
        setupLayoutButtons()
        scanForPdfFiles()
    }

    private fun setupRecyclerView() {
        adapter = ListViewAdapter(
            onItemClick = { pdfFile -> openPdfFile(pdfFile) },
            onMoreClick = { pdfFile -> showFileOptions(pdfFile) },
            isGridLayout = (currentLayoutType == LayoutType.GRID)
        )
        
        binding.rcvFile.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ListViewFragment.adapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPdfFiles(newText ?: "")
                return true
            }
        })
    }
    
    private fun setupSortButton() {
        binding.sortBy.setOnClickListener {
            showSortDialog()
        }
        updateSortButtonUI()
    }
    
    private fun updateSortButtonUI() {
        // Cập nhật contentDescription để hiển thị kiểu sắp xếp hiện tại
        binding.sortBy.contentDescription = "Sort by: ${currentSortType.displayName}"
        
        // Có thể thêm tooltip hoặc thay đổi icon tùy theo kiểu sắp xếp
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.sortBy.tooltipText = "Sort by: ${currentSortType.displayName}"
        }
    }
    
    private fun setupLayoutButtons() {
        binding.verticalList.setOnClickListener {
            switchToLinearLayout()
        }
        
        binding.gridList.setOnClickListener {
            switchToGridLayout()
        }
        
        updateLayoutButtonsUI()
    }
    
    private fun switchToLinearLayout() {
        if (currentLayoutType != LayoutType.LINEAR) {
            currentLayoutType = LayoutType.LINEAR
            
            // Remove grid decoration if exists
            gridItemDecoration?.let { decoration ->
                binding.rcvFile.removeItemDecoration(decoration)
            }
            
            // Recreate adapter with linear layout
            val currentFiles = allPdfFiles.toList()
            adapter = ListViewAdapter(
                onItemClick = { pdfFile -> openPdfFile(pdfFile) },
                onMoreClick = { pdfFile -> showFileOptions(pdfFile) },
                isGridLayout = false
            )
            
            binding.rcvFile.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = this@ListViewFragment.adapter
            }
            
            // Update adapter with current files
            adapter.updatePdfFiles(currentFiles)
            updateLayoutButtonsUI()
        }
    }
    
    private fun switchToGridLayout() {
        if (currentLayoutType != LayoutType.GRID) {
            currentLayoutType = LayoutType.GRID
            
            // Remove existing decoration if any
            gridItemDecoration?.let { decoration ->
                binding.rcvFile.removeItemDecoration(decoration)
            }
            
            // Create and add new grid decoration
            val spacingInPixels = (4 * resources.displayMetrics.density).toInt() // 4dp in pixels
            gridItemDecoration = GridSpacingItemDecoration(2, spacingInPixels, true)
            gridItemDecoration?.let { decoration ->
                binding.rcvFile.addItemDecoration(decoration)
            }
            
            // Recreate adapter with grid layout
            val currentFiles = allPdfFiles.toList()
            adapter = ListViewAdapter(
                onItemClick = { pdfFile -> openPdfFile(pdfFile) },
                onMoreClick = { pdfFile -> showFileOptions(pdfFile) },
                isGridLayout = true
            )
            
            binding.rcvFile.apply {
                layoutManager = GridLayoutManager(requireContext(), 2)
                adapter = this@ListViewFragment.adapter
            }
            
            // Update adapter with current files
            adapter.updatePdfFiles(currentFiles)
            updateLayoutButtonsUI()
        }
    }
    
    private fun updateLayoutButtonsUI() {
        // Cập nhật alpha để hiển thị button nào đang active
        when (currentLayoutType) {
            LayoutType.LINEAR -> {
                binding.verticalList.alpha = 1.0f
                binding.gridList.alpha = 0.5f
            }
            LayoutType.GRID -> {
                binding.verticalList.alpha = 0.5f
                binding.gridList.alpha = 1.0f
            }
        }
    }
    
    private fun showSortDialog() {
        val sortDialog = SortOptionsBottomSheetDialog(
            context = requireContext(),
            currentSortType = currentSortType,
            onSortSelected = { selectedSortType ->
                currentSortType = selectedSortType
                sortPdfFiles()
            }
        )
        sortDialog.show()
    }
    
    private fun sortPdfFiles() {
        val sortedList = when (currentSortType) {
            SortType.NAME_A_TO_Z -> allPdfFiles.sortedBy { it.name.lowercase() }
            SortType.NAME_Z_TO_A -> allPdfFiles.sortedByDescending { it.name.lowercase() }
            SortType.TIME_OLD_TO_NEW -> allPdfFiles.sortedBy { it.lastModified }
            SortType.TIME_NEW_TO_OLD -> allPdfFiles.sortedByDescending { it.lastModified }
            SortType.SIZE_SMALL_TO_LARGE -> allPdfFiles.sortedBy { it.size }
            SortType.SIZE_LARGE_TO_SMALL -> allPdfFiles.sortedByDescending { it.size }
        }
        
        allPdfFiles.clear()
        allPdfFiles.addAll(sortedList)
        
        // Cập nhật UI của nút sort
        updateSortButtonUI()
        
        // Áp dụng lại filter nếu có search query
        val currentQuery = binding.searchView.query.toString()
        if (currentQuery.isNotEmpty()) {
            filterPdfFiles(currentQuery)
        } else {
            adapter.updatePdfFiles(allPdfFiles)
            updateUI(allPdfFiles)
        }
    }

    private fun filterPdfFiles(query: String) {
        val filteredList = if (query.isEmpty()) {
            allPdfFiles
        } else {
            allPdfFiles.filter { 
                it.name.contains(query, ignoreCase = true) 
            }
        }
        
        adapter.updatePdfFiles(filteredList)
        updateUI(filteredList)
    }

    private fun updateUI(pdfFiles: List<PdfFile>) {
        binding.tvFileCount.text = "${pdfFiles.size} Files"
        
        if (pdfFiles.isEmpty()) {
            binding.tvNoFiles.visibility = View.VISIBLE
            binding.rcvFile.visibility = View.GONE
        } else {
            binding.tvNoFiles.visibility = View.GONE
            binding.rcvFile.visibility = View.VISIBLE
        }
    }

    private fun scanForPdfFiles() {
        CoroutineScope(Dispatchers.IO).launch {
            val pdfFiles = mutableListOf<PdfFile>()
            
            // Các thư mục phổ biến chứa file PDF
            val directories = listOf(
                Environment.getExternalStorageDirectory(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                File(Environment.getExternalStorageDirectory(), "Documents"),
                File(Environment.getExternalStorageDirectory(), "PDF")
            )
            
            directories.forEach { directory ->
                if (directory?.exists() == true && directory.isDirectory) {
                    scanDirectory(directory, pdfFiles)
                }
            }
            
            withContext(Dispatchers.Main) {
                allPdfFiles.clear()
                allPdfFiles.addAll(pdfFiles)
                sortPdfFiles() // Áp dụng sắp xếp mặc định
            }
        }
    }

    private fun scanDirectory(directory: File, pdfFiles: MutableList<PdfFile>) {
        try {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isDirectory -> scanDirectory(file, pdfFiles)
                    file.isFile && file.extension.lowercase() == "pdf" -> {
                        val pdfFile = PdfFile(file)
                        if (pdfFile.isValid()) {
                            pdfFiles.add(pdfFile)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // Ignore directories we can't access
        }
    }

    private fun openPdfFile(pdfFile: PdfFile) {
        try {
            val intent = Intent(requireContext(), PdfViewerActivity::class.java).apply {
                putExtra(PdfViewerActivity.EXTRA_PDF_PATH, pdfFile.file.absolutePath)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Không thể mở file PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdfFile(pdfFile: PdfFile) {
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7+ - Sử dụng FileProvider
                FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    pdfFile.file
                )
            } else {
                // Android 6 và thấp hơn - Sử dụng Uri.fromFile()
                Uri.fromFile(pdfFile.file)
            }
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(intent, "Chia sẻ PDF"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Không thể chia sẻ file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFileOptions(pdfFile: PdfFile) {
        val dialog = FileOptionsBottomSheetDialog(
            context = requireContext(),
            pdfFile = pdfFile,
            onRename = { file -> showRenameDialog(file) },
            onShare = { file -> sharePdfFile(file) },
            onDelete = { file -> showDeleteConfirmDialog(file) }
        )
        dialog.show()
    }

    private fun showFileInfo(pdfFile: PdfFile) {
        AlertDialog.Builder(requireContext())
            .setTitle("Thông tin file")
            .setMessage("""
                Tên: ${pdfFile.getNameWithoutExtension()}
                Kích thước: ${pdfFile.getFormattedSize()}
                Ngày sửa đổi: ${pdfFile.getFormattedDate()}
                Đường dẫn: ${pdfFile.path}
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDeleteConfirmDialog(pdfFile: PdfFile) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa file \"${pdfFile.getNameWithoutExtension()}\" không?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteFile(pdfFile)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteFile(pdfFile: PdfFile) {
        try {
            var deleted = false
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ - Sử dụng File.delete() với MANAGE_EXTERNAL_STORAGE
                // hoặc MediaStore với user confirmation
                if (Environment.isExternalStorageManager()) {
                    // Có quyền MANAGE_EXTERNAL_STORAGE
                    deleted = pdfFile.file.delete()
                } else {
                    // Sử dụng MediaStore với user confirmation
                    val contentResolver = requireContext().contentResolver
                    val uri = MediaStore.Files.getContentUri("external")
                    
                    val projection = arrayOf(MediaStore.Files.FileColumns._ID)
                    val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
                    val selectionArgs = arrayOf(pdfFile.file.absolutePath)
                    
                    contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                            val fileUri = Uri.withAppendedPath(uri, id.toString())
                            val deletedRows = contentResolver.delete(fileUri, null, null)
                            deleted = deletedRows > 0
                        }
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 - Sử dụng MediaStore API
                val contentResolver = requireContext().contentResolver
                val uri = MediaStore.Files.getContentUri("external")
                
                val projection = arrayOf(MediaStore.Files.FileColumns._ID)
                val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
                val selectionArgs = arrayOf(pdfFile.file.absolutePath)
                
                contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                        val fileUri = Uri.withAppendedPath(uri, id.toString())
                        val deletedRows = contentResolver.delete(fileUri, null, null)
                        deleted = deletedRows > 0
                    }
                }
            } else {
                // Android 9 và thấp hơn - Sử dụng File.delete()
                deleted = pdfFile.file.delete()
            }
            
            if (deleted) {
                // Xóa khỏi danh sách
                val position = allPdfFiles.indexOf(pdfFile)
                if (position != -1) {
                    allPdfFiles.removeAt(position)
                    adapter.updatePdfFiles(allPdfFiles)
                    updateUI(allPdfFiles)
                }
                Toast.makeText(requireContext(), "Đã xóa file thành công", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Không thể xóa file. Có thể cần cấp quyền quản lý file.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi khi xóa file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRenameDialog(pdfFile: PdfFile) {
        val editText = EditText(requireContext()).apply {
            setText(pdfFile.getNameWithoutExtension())
            selectAll()
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(editText)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Đổi tên file")
            .setView(layout)
            .setPositiveButton("Đổi tên") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != pdfFile.getNameWithoutExtension()) {
                    renameFile(pdfFile, newName)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun renameFile(pdfFile: PdfFile, newName: String) {
        try {
            val newFileName = if (newName.endsWith(".pdf", ignoreCase = true)) {
                newName
            } else {
                "$newName.pdf"
            }
            
            val newFile = File(pdfFile.file.parent, newFileName)
            
            if (newFile.exists()) {
                Toast.makeText(requireContext(), "File với tên này đã tồn tại", Toast.LENGTH_SHORT).show()
                return
            }
            
            var renamed = false
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ - Sử dụng File.renameTo() với MANAGE_EXTERNAL_STORAGE
                // hoặc MediaStore với user confirmation
                if (Environment.isExternalStorageManager()) {
                    // Có quyền MANAGE_EXTERNAL_STORAGE
                    renamed = pdfFile.file.renameTo(newFile)
                } else {
                    // Sử dụng MediaStore với user confirmation
                    val contentResolver = requireContext().contentResolver
                    val uri = MediaStore.Files.getContentUri("external")
                    
                    val projection = arrayOf(MediaStore.Files.FileColumns._ID)
                    val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
                    val selectionArgs = arrayOf(pdfFile.file.absolutePath)
                    
                    contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                            val fileUri = Uri.withAppendedPath(uri, id.toString())
                            
                            val contentValues = ContentValues().apply {
                                put(MediaStore.Files.FileColumns.DISPLAY_NAME, newFileName)
                            }
                            
                            val updatedRows = contentResolver.update(fileUri, contentValues, null, null)
                            if (updatedRows > 0) {
                                // Cũng cần rename file thực tế
                                renamed = pdfFile.file.renameTo(newFile)
                            }
                        }
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 - Sử dụng MediaStore API
                val contentResolver = requireContext().contentResolver
                val uri = MediaStore.Files.getContentUri("external")
                
                val projection = arrayOf(MediaStore.Files.FileColumns._ID)
                val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
                val selectionArgs = arrayOf(pdfFile.file.absolutePath)
                
                contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                        val fileUri = Uri.withAppendedPath(uri, id.toString())
                        
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Files.FileColumns.DISPLAY_NAME, newFileName)
                        }
                        
                        val updatedRows = contentResolver.update(fileUri, contentValues, null, null)
                        if (updatedRows > 0) {
                            // Cũng cần rename file thực tế
                            renamed = pdfFile.file.renameTo(newFile)
                        }
                    }
                }
            } else {
                // Android 9 và thấp hơn - Sử dụng File.renameTo()
                renamed = pdfFile.file.renameTo(newFile)
            }
            
            if (renamed) {
                // Cập nhật trong danh sách
                val position = allPdfFiles.indexOf(pdfFile)
                if (position != -1) {
                    allPdfFiles[position] = PdfFile(newFile)
                    adapter.updatePdfFiles(allPdfFiles)
                    updateUI(allPdfFiles)
                }
                Toast.makeText(requireContext(), "Đã đổi tên file thành công", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Không thể đổi tên file. Có thể cần cấp quyền quản lý file.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi khi đổi tên file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


}