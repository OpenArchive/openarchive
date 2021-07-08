package net.opendasharchive.openarchive.features.media.preview

import net.opendasharchive.openarchive.db.Media

interface MediaRepository {

    suspend fun getMedia(): List<Media>

    suspend fun uploadMedia(media: Media)

}