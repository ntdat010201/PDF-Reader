package com.example.pdfreader.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.example.pdfreader.R
import com.example.pdfreader.ui.fragment.ListViewFragment
import com.google.android.material.bottomsheet.BottomSheetDialog

class SortOptionsBottomSheetDialog(
    private val context: Context,
    private val currentSortType: ListViewFragment.SortType,
    private val onSortSelected: (ListViewFragment.SortType) -> Unit
) {

    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var dialogView: View

    // Check icons for each sort option
    private lateinit var checkNameAToZ: ImageView
    private lateinit var checkNameZToA: ImageView
    private lateinit var checkTimeOldToNew: ImageView
    private lateinit var checkTimeNewToOld: ImageView
    private lateinit var checkSizeSmallToLarge: ImageView
    private lateinit var checkSizeLargeToSmall: ImageView

    fun show() {
        setupDialog()
        setupViews()
        setupClickListeners()
        updateSelectedOption()
        bottomSheetDialog.show()
    }

    private fun setupDialog() {
        bottomSheetDialog = BottomSheetDialog(context)
        dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_sort_options, null)
        bottomSheetDialog.setContentView(dialogView)
    }

    private fun setupViews() {
        checkNameAToZ = dialogView.findViewById(R.id.check_name_a_to_z)
        checkNameZToA = dialogView.findViewById(R.id.check_name_z_to_a)
        checkTimeOldToNew = dialogView.findViewById(R.id.check_time_old_to_new)
        checkTimeNewToOld = dialogView.findViewById(R.id.check_time_new_to_old)
        checkSizeSmallToLarge = dialogView.findViewById(R.id.check_size_small_to_large)
        checkSizeLargeToSmall = dialogView.findViewById(R.id.check_size_large_to_small)
    }

    private fun setupClickListeners() {
        dialogView.findViewById<View>(R.id.option_name_a_to_z).setOnClickListener {
            selectSortOption(ListViewFragment.SortType.NAME_A_TO_Z)
        }

        dialogView.findViewById<View>(R.id.option_name_z_to_a).setOnClickListener {
            selectSortOption(ListViewFragment.SortType.NAME_Z_TO_A)
        }

        dialogView.findViewById<View>(R.id.option_time_old_to_new).setOnClickListener {
            selectSortOption(ListViewFragment.SortType.TIME_OLD_TO_NEW)
        }

        dialogView.findViewById<View>(R.id.option_time_new_to_old).setOnClickListener {
            selectSortOption(ListViewFragment.SortType.TIME_NEW_TO_OLD)
        }

        dialogView.findViewById<View>(R.id.option_size_small_to_large).setOnClickListener {
            selectSortOption(ListViewFragment.SortType.SIZE_SMALL_TO_LARGE)
        }

        dialogView.findViewById<View>(R.id.option_size_large_to_small).setOnClickListener {
            selectSortOption(ListViewFragment.SortType.SIZE_LARGE_TO_SMALL)
        }
    }

    private fun selectSortOption(sortType: ListViewFragment.SortType) {
        onSortSelected(sortType)
        bottomSheetDialog.dismiss()
    }

    private fun updateSelectedOption() {
        // Hide all check icons first
        hideAllCheckIcons()

        // Show check icon for current selected option
        when (currentSortType) {
            ListViewFragment.SortType.NAME_A_TO_Z -> checkNameAToZ.visibility = View.VISIBLE
            ListViewFragment.SortType.NAME_Z_TO_A -> checkNameZToA.visibility = View.VISIBLE
            ListViewFragment.SortType.TIME_OLD_TO_NEW -> checkTimeOldToNew.visibility = View.VISIBLE
            ListViewFragment.SortType.TIME_NEW_TO_OLD -> checkTimeNewToOld.visibility = View.VISIBLE
            ListViewFragment.SortType.SIZE_SMALL_TO_LARGE -> checkSizeSmallToLarge.visibility = View.VISIBLE
            ListViewFragment.SortType.SIZE_LARGE_TO_SMALL -> checkSizeLargeToSmall.visibility = View.VISIBLE
        }
    }

    private fun hideAllCheckIcons() {
        checkNameAToZ.visibility = View.INVISIBLE
        checkNameZToA.visibility = View.INVISIBLE
        checkTimeOldToNew.visibility = View.INVISIBLE
        checkTimeNewToOld.visibility = View.INVISIBLE
        checkSizeSmallToLarge.visibility = View.INVISIBLE
        checkSizeLargeToSmall.visibility = View.INVISIBLE
    }
}