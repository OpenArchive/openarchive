package net.opendasharchive.openarchive.features.media.preview

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import net.opendasharchive.openarchive.features.media.MediaWorker

class PreviewMediaListViewModel(
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableLiveData<UIState>()
    val uiState: LiveData<UIState>
        get() = _uiState

    val workState: LiveData<List<WorkInfo>>

    private val workManager = WorkManager.getInstance(application)
    private val TAG_MEDIA_UPLOADING = "TAG_MEDIA_UPLOADING"

    init {
        workState = workManager.getWorkInfosByTagLiveData(TAG_MEDIA_UPLOADING)
    }

    fun applyMedia() {
        val mediaWorker = OneTimeWorkRequestBuilder<MediaWorker>().addTag(TAG_MEDIA_UPLOADING).build()
        workManager.enqueue(mediaWorker)
    }


}