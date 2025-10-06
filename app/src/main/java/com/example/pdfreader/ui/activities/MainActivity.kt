package com.example.pdfreader.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pdfreader.R
import com.example.pdfreader.databinding.ActivityMainBinding
import com.example.pdfreader.ui.fragment.ListViewFragment
import com.example.pdfreader.utils.PermissionManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionManager = PermissionManager(this)
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
        if (permissionManager.hasStoragePermission()) {
            loadListViewFragment()
        } else {
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {
        permissionManager.requestStoragePermission { granted ->
            if (granted) {
                loadListViewFragment()
            } else {
                permissionManager.showPermissionDeniedDialog()
            }
        }
    }


}