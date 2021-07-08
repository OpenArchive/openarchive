package net.opendasharchive.openarchive.features.media.preview

sealed class UIState {
    object Loading: UIState()
    data class Success(val message: String): UIState()
    data class Error(val message: String): UIState()
}