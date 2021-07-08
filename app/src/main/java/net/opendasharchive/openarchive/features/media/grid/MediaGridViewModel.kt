package net.opendasharchive.openarchive.features.media.grid

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Collection.Companion.getAllAsList

class MediaGridViewModel : ViewModel() {

    private val _collections = MutableLiveData<List<Collection>?>()
    val collections: LiveData<List<Collection>?>
        get() = _collections

    fun getAllCollection() {
        viewModelScope.launch {
            _collections.value = getAllAsList()
        }
    }
}