package net.opendasharchive.openarchive.features.media.preview

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.orm.SugarRecord
import com.orm.SugarRecord.*
import io.scal.secureshareui.controller.ArchiveSiteController
import io.scal.secureshareui.controller.SiteController
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.publish.UploaderListenerV2
import net.opendasharchive.openarchive.services.dropbox.DropboxSiteController
import net.opendasharchive.openarchive.services.webdav.WebDAVSiteController
import net.opendasharchive.openarchive.util.Constants
import java.util.*

class MediaRepositoryImpl(
    private val ctx: Context
) : MediaRepository {

    private val datePublish = Date()

    override suspend fun getMedia(): List<Media> {
        val where = "status = ? OR status = ?"
        val whereArgs = arrayOf("${Media.STATUS_QUEUED}", "${Media.STATUS_UPLOADING}")

        return find(
            Media::class.java, where, whereArgs, null, "priority DESC", null
        )
    }

    override suspend fun uploadMedia(media: Media) {
        val coll = findById<Collection>(
            Collection::class.java, media.collectionId
        )
        val proj = findById<Project>(
            Project::class.java,
            coll.projectId
        )

        proj?.let {
            if (media.status != Media.STATUS_UPLOADING) {
                media.uploadDate = datePublish
                media.progress = 0 //should we reset this?
                media.status = Media.STATUS_UPLOADING
                media.statusMessage = Constants.EMPTY_STRING
            }

            media.licenseUrl = proj.licenseUrl

            val project = Project.getById(media.projectId)
            project?.let {
                val valueMap = ArchiveSiteController.getMediaMetadata(ctx, media)
                media.serverUrl = project.description ?: Constants.EMPTY_STRING
                media.status = Media.STATUS_UPLOADING
                media.save()
                //notifyMediaUpdated(media)

                var space: Space? = if (project.spaceId != -1L) findById<Space>(
                    Space::class.java, project.spaceId
                ) else Space.getCurrentSpace()

                space?.let {

                    var sc: SiteController? = null

                    try {
                        when (space.type) {
                            Space.TYPE_WEBDAV -> sc =
                                SiteController.getSiteController(
                                    WebDAVSiteController.SITE_KEY,
                                    ctx,
                                    UploaderListenerV2(media, ctx),
                                    null
                                )
                            Space.TYPE_INTERNET_ARCHIVE -> sc =
                                SiteController.getSiteController(
                                    ArchiveSiteController.SITE_KEY,
                                    ctx,
                                    UploaderListenerV2(media, ctx),
                                    null
                                )
                            Space.TYPE_DROPBOX -> sc =
                                SiteController.getSiteController(
                                    DropboxSiteController.SITE_KEY,
                                    ctx,
                                    UploaderListenerV2(media, ctx),
                                    null
                                )
                        }
                        val result = sc?.upload(space, media, valueMap)
                        if (result == true) {
                            if (coll != null) {
                                coll.uploadDate = datePublish
                                coll.save()
                                proj.openCollectionId = -1L
                                proj.save()
                            }
                            media.save()
                        } else {

                        }
                    } catch (ex: Exception) {
                        val err = "error in uploading media: " + ex.message
                        Log.d(javaClass.name, err, ex)

                        media.statusMessage = err

                        media.status = Media.STATUS_ERROR
                        media.save()
                        throw Exception(err)
                    }
                }
            } ?: run {
                media.delete()
                throw Exception()
            }
        } ?: run {
            media.status = Media.STATUS_LOCAL
        }
    }

    // Send an Intent with an action named "custom-event-name". The Intent sent should
    // be received by the ReceiverActivity.
    private fun notifyMediaUpdated(media: Media) {
        Log.d("sender", "Broadcasting message")
        val intent = Intent(MainActivity.INTENT_FILTER_NAME)
        // You can also include some extra data.
        intent.putExtra(SiteController.MESSAGE_KEY_MEDIA_ID, media.id)
        intent.putExtra(SiteController.MESSAGE_KEY_STATUS, media.status)
        intent.putExtra(SiteController.MESSAGE_KEY_PROGRESS, media.progress)
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent)
    }

}