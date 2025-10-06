package com.example.pdfreader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfreader.R
import com.example.pdfreader.model.PdfFile

class ListViewAdapter(
    private var pdfFiles: MutableList<PdfFile> = mutableListOf(),
    private val onItemClick: (PdfFile) -> Unit = {},
    private val onMoreClick: (PdfFile) -> Unit = {},
    private var isGridLayout: Boolean = false
) : RecyclerView.Adapter<ListViewAdapter.PdfViewHolder>() {

    inner class PdfViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileIcon: ImageView = itemView.findViewById(R.id.file_icon)
        val fileName: TextView = itemView.findViewById(R.id.file_name)
        val lastModified: TextView = itemView.findViewById(R.id.last_modified)
        val sizeFile: TextView = itemView.findViewById(R.id.size_file)
        val moreVert: ImageView = itemView.findViewById(R.id.more_vert)

        fun bind(pdfFile: PdfFile) {
            fileName.text = pdfFile.getNameWithoutExtension()
            lastModified.text = pdfFile.getFormattedDate()
            sizeFile.text = pdfFile.getFormattedSize()

            // Set click listeners
            itemView.setOnClickListener {
                onItemClick(pdfFile)
            }

            moreVert.setOnClickListener {
                onMoreClick(pdfFile)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val layoutRes = if (isGridLayout) R.layout.item_file_grid else R.layout.item_file
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)
        return PdfViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.bind(pdfFiles[position])
    }

    override fun getItemCount(): Int = pdfFiles.size

    /**
     * Cập nhật danh sách PDF files
     */
    fun updatePdfFiles(newPdfFiles: List<PdfFile>) {
        pdfFiles.clear()
        pdfFiles.addAll(newPdfFiles)
        notifyDataSetChanged()
    }

    /**
     * Thêm một file PDF vào danh sách
     */
    fun addPdfFile(pdfFile: PdfFile) {
        pdfFiles.add(pdfFile)
        notifyItemInserted(pdfFiles.size - 1)
    }

    /**
     * Xóa một file PDF khỏi danh sách
     */
    fun removePdfFile(position: Int) {
        if (position in 0 until pdfFiles.size) {
            pdfFiles.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * Lấy file PDF tại vị trí cụ thể
     */
    fun getPdfFile(position: Int): PdfFile? {
        return if (position in 0 until pdfFiles.size) pdfFiles[position] else null
    }

    /**
     * Lọc danh sách theo tên file
     */
    fun filter(query: String) {
        val filteredList = if (query.isEmpty()) {
            pdfFiles
        } else {
            pdfFiles.filter { 
                it.name.contains(query, ignoreCase = true) 
            }
        }
        updatePdfFiles(filteredList)
    }
    
    /**
     * Cập nhật layout type (grid hoặc linear)
     */
    fun setGridLayout(isGrid: Boolean) {
        if (isGridLayout != isGrid) {
            isGridLayout = isGrid
            notifyDataSetChanged()
        }
    }
}