package net.opendasharchive.openarchive.features.media.review

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException

class ReviewMediaViewModelFactory(
    private val application: Application
): ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReviewMediaViewModel::class.java)) {
            return ReviewMediaViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}