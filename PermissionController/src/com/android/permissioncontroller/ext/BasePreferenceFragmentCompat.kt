package com.android.permissioncontroller.ext

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.preference.PreferenceFragmentCompat
import com.android.permissioncontroller.permission.ui.handheld.pressBack

abstract class BasePreferenceFragmentCompat : PreferenceFragmentCompat() {
    val context_: Context
        get() = requireContext()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION") // see onOptionsItemSelected
        setHasOptionsMenu(true) // needed for the "back arrow" button even when there's no menu
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(context_)
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    override fun onStart() {
        super.onStart()
        requireActivity().title = getTitle()
    }

    // it's not clear how to resolve deprecation warnings for setHasOptionsMenu and onOptionsItemSelected,
    // they are suppressed in upstream fragments that use android.R.id.home too
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            this.pressBack()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    abstract fun update()

    abstract fun getTitle(): CharSequence

    val toastManager = ToastManager(this)
}
