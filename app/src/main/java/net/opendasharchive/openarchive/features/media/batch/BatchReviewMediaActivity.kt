package net.opendasharchive.openarchive.features.media.batch

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso
import com.stfalcon.frescoimageviewer.ImageViewer
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityBatchReviewMediaBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Media.Companion.getMediaById
import net.opendasharchive.openarchive.fragments.VideoRequestHandler
import net.opendasharchive.openarchive.util.Constants.EMPTY_STRING
import net.opendasharchive.openarchive.util.Globals
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import java.io.File
import java.util.*

class BatchReviewMediaActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityBatchReviewMediaBinding
    private lateinit var viewModel: BatchReviewMediaViewModel

    private var mediaList: ArrayList<Media> = arrayListOf()
    private var mPicasso: Picasso? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityBatchReviewMediaBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        viewModel = ViewModelProvider(this).get(BatchReviewMediaViewModel::class.java)
        initLayout()
    }

    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = EMPTY_STRING
        }

        if (mPicasso == null) {
            val videoRequestHandler = VideoRequestHandler(this)

            mPicasso = Picasso.Builder(this)
                .addRequestHandler(videoRequestHandler)
                .build()
        }

    }

    private fun updateFlagState() {
        mediaList.forEach { media ->
            media.flag = !mediaList[0].flag
        }
        updateFlagState(mediaList[0])
    }

    private fun updateFlagState(media: Media) {
        if (media.flag) mBinding.archiveMetadataLayout.ivEditFlag.setImageResource(R.drawable.ic_flag_selected) else mBinding.archiveMetadataLayout.ivEditFlag.setImageResource(
            R.drawable.ic_flag_unselected
        )
        if (media.flag) mBinding.archiveMetadataLayout.tvFlagLbl.setText(R.string.status_flagged) else mBinding.archiveMetadataLayout.tvFlagLbl.setText(
            R.string.hint_flag
        )
        if ((media.status != Media.STATUS_LOCAL
                    && media.status != Media.STATUS_NEW) && !media.flag
        ) {
            mBinding.archiveMetadataLayout.ivEditFlag.hide()
            mBinding.archiveMetadataLayout.tvFlagLbl.hide()
        }
    }

    private fun bindMedia() {
        bindMedia(mediaList[0])
        mBinding.itemDisplay.removeAllViews()
        for (media in mediaList) showThumbnail(media)
    }

    private fun bindMedia(media: Media) {
        // set values

        mBinding.apply {
            archiveMetadataLayout.apply {
                tvTitleLbl.setText(media.title)

                if (media.description.isNotEmpty()) {
                    tvDescriptionLbl.setText(media.description)
                    ivEditNotes.setImageResource(R.drawable.ic_edit_selected)
                }

                if (media.location.isNotEmpty()) {
                    tvLocationLbl.setText(media.location)
                    ivEditLocation.setImageResource(R.drawable.ic_location_selected)
                }

                if (!media.getTags().isNullOrEmpty()) {
                    tvTagsLbl.setText(media.getTags())
                    ivEditTags.setImageResource(R.drawable.ic_tag_selected)
                }

                tvAuthorLbl.setText(media.author)
                tvCcLicense.setText(media.licenseUrl)
            }

            if (media.status != Media.STATUS_LOCAL && media.status != Media.STATUS_NEW) {
                if (media.status == Media.STATUS_UPLOADED || media.status == Media.STATUS_PUBLISHED) {
                    // NO-OP
                } else if (media.status == Media.STATUS_QUEUED) {
                    tvUrl.text = getString(R.string.batch_waiting_for_upload)
                    tvUrl.show()
                } else if (media.status == Media.STATUS_UPLOADING) {
                    tvUrl.text = getString(R.string.batch_uploading_now)
                    tvUrl.show()
                }

                archiveMetadataLayout.apply {
                    tvCcLicense.movementMethod = LinkMovementMethod.getInstance()
                    tvTitleLbl.isEnabled = false
                    tvDescriptionLbl.isEnabled = false

                    if (media.description.isEmpty()) {
                        ivEditNotes.hide()
                        tvDescriptionLbl.hint = EMPTY_STRING
                    }

                    tvAuthorLbl.isEnabled = false
                    tvLocationLbl.isEnabled = false

                    if (TextUtils.isEmpty(media.location)) {
                        ivEditLocation.hide()
                        tvLocationLbl.hint = EMPTY_STRING
                    }

                    tvTagsLbl.isEnabled = false

                    if (media.getTags().isNullOrEmpty()) {
                        ivEditTags.hide()
                        tvTagsLbl.hint = EMPTY_STRING
                    }

                    tvCcLicense.isEnabled = false
                    groupLicenseChooser.hide()
                }

            } else {
                archiveMetadataLayout.rowFlag.setOnClickListener { updateFlagState() }
            }
            updateFlagState(media)
        }
    }

    private fun saveMedia() {
        for (media in mediaList) saveMedia(media)
    }

    private fun saveMedia(media: Media?) {
        //if deleted
        if (media == null) return

        mBinding.archiveMetadataLayout.let { metaDataLayout ->

            val title = if (metaDataLayout.tvTitleLbl.text.isNotEmpty())
                metaDataLayout.tvTitleLbl.text.toString() else {
                //use the file name if the user doesn't set a title
                File(media.originalFilePath).name
            }

            viewModel.saveMedia(
                media,
                title,
                metaDataLayout.tvDescriptionLbl.text.toString(),
                metaDataLayout.tvAuthorLbl.text.toString(),
                metaDataLayout.tvLocationLbl.text.toString(),
                metaDataLayout.tvTagsLbl.text.toString()
            )
        }
    }

    override fun onPause() {
        super.onPause()
        saveMedia()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun init() {
        val intent = intent
        val mediaIds = intent.getLongArrayExtra(Globals.EXTRA_CURRENT_MEDIA_ID)
        mediaList = ArrayList()
        mediaIds?.forEach { mediaId ->
            mediaList.add(getMediaById(mediaId))
        }
        // get default metadata sharing values
        val sharedPref = getSharedPreferences(Globals.PREF_FILE_KEY, Context.MODE_PRIVATE)
        bindMedia()
    }

    private fun showThumbnail(media: Media) {
        val ivMedia = ImageView(this)
        val margin = 3
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        lp.setMargins(margin, margin, margin, margin)
        lp.height = 600
        lp.width = 800
        ivMedia.layoutParams = lp
        ivMedia.scaleType = ImageView.ScaleType.CENTER_CROP
        if (media.mimeType.startsWith("image")) {
            Glide.with(ivMedia.context).load(Uri.parse(media.originalFilePath)).into(ivMedia)
        } else if (media.mimeType.startsWith("video")) {
            mPicasso?.load(VideoRequestHandler.SCHEME_VIDEO + ":" + media.originalFilePath)?.fit()
                ?.centerCrop()?.into(ivMedia)
        } else if (media.mimeType.startsWith("audio")) {
            ivMedia.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.audio_waveform))
        } else ivMedia.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.no_thumbnail))
        mBinding.itemDisplay.addView(ivMedia)
    }

    private fun showMedia(media: Media) {
        if (media.mimeType.startsWith("image")) {
            val list = ArrayList<Uri>()
            list.add(Uri.parse(media.originalFilePath))
            ImageViewer.Builder(this, list)
                .setStartPosition(0)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        init()
        bindMedia()
    }

    override fun onDestroy() {
        // Unregister since the activity is about to be closed.
        super.onDestroy()
    }

    companion object {
        const val TAG = "ReviewMediaActivity"
    }
}