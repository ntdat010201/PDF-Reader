package com.example.pdfreader.ui.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.pdfreader.databinding.ActivityPdfViewerBinding
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import java.io.File

class PdfViewerActivity : AppCompatActivity(), OnPageChangeListener, OnLoadCompleteListener,
    OnPageErrorListener {

    private lateinit var binding: ActivityPdfViewerBinding
    private var pdfFile: File? = null
    private var pageNumber = 0
    private var totalPages = 0
    private var currentZoom = 1.0f

    companion object {
        const val EXTRA_PDF_PATH = "pdf_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        loadPdfFromIntent()
    }

    private fun setupToolbar() {
        binding.imgBack.setOnClickListener {
            finish()
        }
        
        // Setup modern back press handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun setupClickListeners() {
        binding.previousPage.setOnClickListener {
            if (pageNumber > 0) {
                pageNumber--
                binding.pdfView.jumpTo(pageNumber, true)
                // Thêm hiệu ứng animation nhẹ
                it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100)
                    .withEndAction {
                        it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100)
                    }
            } else {
                // Feedback khi đã ở trang đầu
                it.animate().translationX(-10f).setDuration(100)
                    .withEndAction {
                        it.animate().translationX(10f).setDuration(100)
                            .withEndAction {
                                it.animate().translationX(0f).setDuration(100)
                            }
                    }
            }
        }

        binding.next.setOnClickListener {
            if (pageNumber < totalPages - 1) {
                pageNumber++
                binding.pdfView.jumpTo(pageNumber, true)
                // Thêm hiệu ứng animation nhẹ
                it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100)
                    .withEndAction {
                        it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100)
                    }
            } else {
                // Feedback khi đã ở trang cuối
                it.animate().translationX(10f).setDuration(100)
                    .withEndAction {
                        it.animate().translationX(-10f).setDuration(100)
                            .withEndAction {
                                it.animate().translationX(0f).setDuration(100)
                            }
                    }
            }
        }

        binding.zoomIn.setOnClickListener {
            val newZoom = (currentZoom * 1.25f).coerceAtMost(3.0f)
            if (newZoom > currentZoom) {
                // Áp dụng zoom ngay lập tức với animation mượt mà
                binding.pdfView.zoomWithAnimation(newZoom)
                
                // Cập nhật currentZoom sau khi animation hoàn thành
                Handler(Looper.getMainLooper()).postDelayed({
                    currentZoom = binding.pdfView.zoom
                }, 300) // Đợi animation hoàn thành
                
                // Animation feedback cho nút
                it.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150)
                    .withEndAction {
                        it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150)
                    }
            } else {
                // Feedback khi đã zoom tối đa
                it.animate().rotationBy(15f).setDuration(100)
                    .withEndAction {
                        it.animate().rotationBy(-30f).setDuration(100)
                            .withEndAction {
                                it.animate().rotationBy(15f).setDuration(100)
                            }
                    }
            }
        }

        binding.zoomOut.setOnClickListener {
            val newZoom = (currentZoom / 1.25f).coerceAtLeast(0.5f)
            if (newZoom < currentZoom) {
                // Áp dụng zoom ngay lập tức với animation mượt mà
                binding.pdfView.zoomWithAnimation(newZoom)
                
                // Cập nhật currentZoom sau khi animation hoàn thành
                Handler(Looper.getMainLooper()).postDelayed({
                    currentZoom = binding.pdfView.zoom
                }, 300) // Đợi animation hoàn thành
                
                // Animation feedback cho nút
                it.animate().scaleX(0.8f).scaleY(0.8f).setDuration(150)
                    .withEndAction {
                        it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150)
                    }
            } else {
                // Feedback khi đã zoom tối thiểu
                it.animate().rotationBy(-15f).setDuration(100)
                    .withEndAction {
                        it.animate().rotationBy(30f).setDuration(100)
                            .withEndAction {
                                it.animate().rotationBy(-15f).setDuration(100)
                            }
                    }
            }
        }
    }

    private fun loadPdfFromIntent() {
        val pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
        if (pdfPath != null) {
            pdfFile = File(pdfPath)
            if (pdfFile?.exists() == true) {
                loadPdf()
            } else {
                Toast.makeText(this, "File không tồn tại", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Không tìm thấy đường dẫn file", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadPdf() {
        pdfFile?.let { file ->
            binding.pdfView.fromFile(file)
                .defaultPage(pageNumber)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .scrollHandle(DefaultScrollHandle(this))
                .spacing(10) // in dp
                .onPageError(this)
                .load()

            // Hiển thị tên file
            binding.tvFileName.text = file.name
        }
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        pageNumber = page
        totalPages = pageCount
        binding.tvPage.text = "${page + 1} of $pageCount"
    }

    override fun loadComplete(nbPages: Int) {
        totalPages = nbPages
        Toast.makeText(this, "PDF đã tải thành công ($nbPages trang)", Toast.LENGTH_SHORT).show()
    }

    override fun onPageError(page: Int, t: Throwable?) {
        Toast.makeText(this, "Lỗi khi tải trang $page: ${t?.message}", Toast.LENGTH_SHORT).show()
    }
}