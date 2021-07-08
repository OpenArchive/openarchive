package net.opendasharchive.openarchive.features.media.batch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.util.extensions.runOnBackground

class BatchReviewMediaViewModel : ViewModel() {

    fun saveMedia(
        media: Media?,
        title: String,
        description: String,
        author: String,
        location: String,
        tags: String
    ) {
        media?.let {
            viewModelScope.runOnBackground {
                media.title = title
                media.description = description
                media.author = author
                media.location = location
                media.setTags(tags)
                val project = Project.getById(media.projectId)
                media.licenseUrl = project?.licenseUrl
                if (media.status == Media.STATUS_NEW) media.status = Media.STATUS_LOCAL
                media.save()
            }
        }
    }

}