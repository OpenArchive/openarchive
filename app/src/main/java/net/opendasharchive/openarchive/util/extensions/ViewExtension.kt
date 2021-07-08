package net.opendasharchive.openarchive.util.extensions

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.createSnackBar(message: String, duration: Int): Snackbar {
    return Snackbar.make(this, message, duration)
}