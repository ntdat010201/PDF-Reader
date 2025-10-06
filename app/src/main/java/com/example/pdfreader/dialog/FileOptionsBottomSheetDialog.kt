package com.example.pdfreader.dialog

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.pdfreader.R
import com.example.pdfreader.model.PdfFile
import com.google.android.material.bottomsheet.BottomSheetDialog

class FileOptionsBottomSheetDialog(
    private val context: Context,
    private val pdfFile: PdfFile,
    private val onRename: (PdfFile) -> Unit,
    private val onShare: (PdfFile) -> Unit,
    private val onDelete: (PdfFile) -> Unit
) {

    private lateinit var bottomSheetDialog: BottomSheetDialog

    fun show() {
        bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_file_options, null)
        bottomSheetDialog.setContentView(view)

        setupFileInfo(view)
        setupClickListeners(view)

        bottomSheetDialog.show()
    }

    private fun setupFileInfo(view: View) {
        val fileIcon = view.findViewById<ImageView>(R.id.file_icon_header)
        val fileName = view.findViewById<TextView>(R.id.file_name_header)
        val fileInfo = view.findViewById<TextView>(R.id.file_info_header)

        fileName.text = pdfFile.getNameWithoutExtension()
        fileInfo.text = "Today ${pdfFile.getFormattedSize()}"
        // Set PDF icon
        fileIcon.setImageResource(R.drawable.ic_pdf)
    }

    private fun setupClickListeners(view: View) {
        // Save to Device
        view.findViewById<View>(R.id.option_save_to_device).setOnClickListener {
            saveToDevice()
            bottomSheetDialog.dismiss()
        }

        // Select
        view.findViewById<View>(R.id.option_select).setOnClickListener {
            selectFile()
            bottomSheetDialog.dismiss()
        }

        // Rename
        view.findViewById<View>(R.id.option_rename).setOnClickListener {
            onRename(pdfFile)
            bottomSheetDialog.dismiss()
        }

        // Print
        view.findViewById<View>(R.id.option_print).setOnClickListener {
            printFile()
            bottomSheetDialog.dismiss()
        }

        // Share
        view.findViewById<View>(R.id.option_share).setOnClickListener {
            onShare(pdfFile)
            bottomSheetDialog.dismiss()
        }

        // Delete
        view.findViewById<View>(R.id.option_delete).setOnClickListener {
            onDelete(pdfFile)
            bottomSheetDialog.dismiss()
        }
    }

    private fun saveToDevice() {
        Toast.makeText(context, "Đang lưu ${pdfFile.name} vào thiết bị...", Toast.LENGTH_SHORT).show()
        // TODO: Implement actual save to device functionality
    }

    private fun selectFile() {
        Toast.makeText(context, "Đã chọn ${pdfFile.name}", Toast.LENGTH_SHORT).show()
        // TODO: Implement file selection functionality
    }

    private fun printFile() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(android.net.Uri.fromFile(pdfFile.file), "application/pdf")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể mở file để in", Toast.LENGTH_SHORT).show()
        }
    }

    fun dismiss() {
        if (::bottomSheetDialog.isInitialized) {
            bottomSheetDialog.dismiss()
        }
    }
}