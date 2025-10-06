package com.example.pdfreader.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class PermissionManager(private val activity: FragmentActivity) {

    companion object {
        const val REQUEST_CODE_STORAGE_PERMISSION = 100
        const val REQUEST_CODE_MANAGE_EXTERNAL_STORAGE = 101
    }

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    // ActivityResultLauncher cho MANAGE_EXTERNAL_STORAGE (Android 11+)
    private val manageExternalStorageLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
            onPermissionResult?.invoke(hasPermission)
        }

    // ActivityResultLauncher cho quyền thông thường
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            onPermissionResult?.invoke(allGranted)
        }

    /**
     * Kiểm tra xem app có quyền truy cập storage không
     */
    fun hasStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ cần MANAGE_EXTERNAL_STORAGE
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-10 cần READ_EXTERNAL_STORAGE và WRITE_EXTERNAL_STORAGE
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                // Android 5 và thấp hơn không cần runtime permission
                true
            }
        }
    }

    /**
     * Yêu cầu quyền truy cập storage
     */
    fun requestStoragePermission(callback: (Boolean) -> Unit) {
        onPermissionResult = callback

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ yêu cầu MANAGE_EXTERNAL_STORAGE
                requestManageExternalStoragePermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-10 yêu cầu READ_EXTERNAL_STORAGE và WRITE_EXTERNAL_STORAGE
                requestLegacyStoragePermission()
            }
            else -> {
                // Android 5 và thấp hơn không cần runtime permission
                callback(true)
            }
        }
    }

    /**
     * Yêu cầu quyền MANAGE_EXTERNAL_STORAGE cho Android 11+
     */
    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                onPermissionResult?.invoke(true)
                return
            }

            // Hiển thị dialog giải thích tại sao cần quyền này
            AlertDialog.Builder(activity)
                .setTitle("Cần quyền truy cập file")
                .setMessage("Ứng dụng cần quyền quản lý tất cả file để có thể đọc, đổi tên và xóa file PDF. Vui lòng cấp quyền trong cài đặt.")
                .setPositiveButton("Đi đến cài đặt") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:${activity.packageName}")
                        manageExternalStorageLauncher.launch(intent)
                    } catch (e: Exception) {
                        // Fallback nếu intent cụ thể không hoạt động
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        manageExternalStorageLauncher.launch(intent)
                    }
                }
                .setNegativeButton("Hủy") { _, _ ->
                    onPermissionResult?.invoke(false)
                }
                .setCancelable(false)
                .show()
        }
    }

    /**
     * Yêu cầu quyền READ_EXTERNAL_STORAGE và WRITE_EXTERNAL_STORAGE cho Android 6-10
     */
    private fun requestLegacyStoragePermission() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val shouldShowRationale = permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }

        if (shouldShowRationale) {
            // Hiển thị dialog giải thích tại sao cần quyền này
            AlertDialog.Builder(activity)
                .setTitle("Cần quyền truy cập file")
                .setMessage("Ứng dụng cần quyền truy cập bộ nhớ để có thể đọc, đổi tên và xóa file PDF.")
                .setPositiveButton("Cấp quyền") { _, _ ->
                    permissionLauncher.launch(permissions)
                }
                .setNegativeButton("Hủy") { _, _ ->
                    onPermissionResult?.invoke(false)
                }
                .setCancelable(false)
                .show()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    /**
     * Kiểm tra xem có thể yêu cầu quyền hay không
     */
    fun canRequestPermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                true // Luôn có thể yêu cầu MANAGE_EXTERNAL_STORAGE
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val permissions = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                permissions.any { permission ->
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) ||
                    ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
                }
            }
            else -> true
        }
    }

    /**
     * Mở cài đặt ứng dụng để người dùng có thể cấp quyền thủ công
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${activity.packageName}")
        activity.startActivity(intent)
    }

    /**
     * Hiển thị dialog hướng dẫn cấp quyền thủ công
     */
    fun showPermissionDeniedDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Cần quyền truy cập file")
            .setMessage("Ứng dụng cần quyền truy cập file để hoạt động. Vui lòng vào Cài đặt > Ứng dụng > ${activity.getString(activity.applicationInfo.labelRes)} > Quyền và cấp quyền truy cập bộ nhớ.")
            .setPositiveButton("Đi đến cài đặt") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}