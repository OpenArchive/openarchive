package net.opendasharchive.openarchive.util.extensions

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar

fun FragmentActivity.routeTo(fragment: Fragment, containerId: Int) {
    supportFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(fragment::class.java.canonicalName)
            .commit()
}