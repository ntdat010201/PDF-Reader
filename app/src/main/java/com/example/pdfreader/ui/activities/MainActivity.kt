package com.example.pdfreader.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pdfreader.R
import com.example.pdfreader.databinding.ActivityMainBinding
import com.example.pdfreader.ui.fragment.ListViewFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
        
        // Constants cho READ_MEDIA permissions (API 33+)
        private const val READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES"
        private const val READ_MEDIA_VIDEO = "android.permission.READ_MEDIA_VIDEO"
        private const val READ_MEDIA_AUDIO = "android.permission.READ_MEDIA_AUDIO"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestPermissions()
    }

    private fun loadListViewFragment() {
        if (supportFragmentManager.findFragmentById(R.id.container) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ListViewFragment())
                .commit()
        }
    }

    private fun checkAndRequestPermissions() {
        if (hasStoragePermission()) {
            loadListViewFragment()
        } else {
            requestStoragePermission()
        }
    }



    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ cần READ_MEDIA_* permissions
            ContextCompat.checkSelfPermission(this, READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 và thấp hơn dùng READ_EXTERNAL_STORAGE
            val readPermission = ContextCompat.checkSelfPermission(
                this, 
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            
            val writePermission = ContextCompat.checkSelfPermission(
                this, 
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            
            readPermission && writePermission
        }
    }

    private fun requestStoragePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ xin READ_MEDIA_* permissions
            arrayOf(
                READ_MEDIA_IMAGES,
                READ_MEDIA_VIDEO,
                READ_MEDIA_AUDIO
            )
        } else {
            // Android 12 và thấp hơn xin READ/WRITE_EXTERNAL_STORAGE
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        
        ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (hasStoragePermission()) {
                loadListViewFragment()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cần quyền truy cập")
            .setMessage("Ứng dụng cần quyền truy cập bộ nhớ để hiển thị file PDF. Vui lòng cấp quyền trong Cài đặt.")
            .setPositiveButton("Cài đặt") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }


}