package net.opendasharchive.openarchive.features.media.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.util.Constants.EMPTY_ID

class MediaListViewModel : ViewModel() {

    private val _mediaList = MutableLiveData<List<Media>?>()
    val mediaList: LiveData<List<Media>?>
        get() = _mediaList

    fun getMediaList(projectId: Long, status: LongArray) {
        if (projectId == EMPTY_ID) {
            _mediaList.value = Media.getMediaByStatus(status, Media.ORDER_PRIORITY)
        } else {
            _mediaList.value = Media.getMediaByProject(projectId)
        }
    }


}