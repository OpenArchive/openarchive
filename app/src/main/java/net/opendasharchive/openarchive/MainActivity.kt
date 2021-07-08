package net.opendasharchive.openarchive

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.amulyakhare.textdrawable.TextDrawable
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.orm.SugarRecord.findById
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.PicassoEngine
import com.zhihu.matisse.internal.entity.CaptureStrategy
import io.scal.secureshareui.controller.SiteController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.opendasharchive.openarchive.databinding.ActivityMainBinding
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Media.Companion.getMediaByStatus
import net.opendasharchive.openarchive.db.Project.Companion.getAllBySpace
import net.opendasharchive.openarchive.db.ProjectAdapter
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.db.Space.Companion.getAllAsList
import net.opendasharchive.openarchive.db.Space.Companion.getCurrentSpace
import net.opendasharchive.openarchive.features.core.SpaceSettingsActivity
import net.opendasharchive.openarchive.features.media.list.MediaListFragment
import net.opendasharchive.openarchive.features.media.preview.PreviewMediaListActivity
import net.opendasharchive.openarchive.features.media.review.ReviewMediaActivity
import net.opendasharchive.openarchive.features.onboarding.OAAppIntro
import net.opendasharchive.openarchive.features.projects.AddProjectActivity
import net.opendasharchive.openarchive.publish.UploadManagerActivity
import net.opendasharchive.openarchive.ui.BadgeDrawable
import net.opendasharchive.openarchive.util.*
import net.opendasharchive.openarchive.util.extensions.createSnackBar
import net.opendasharchive.openarchive.util.extensions.executeAsyncTask
import net.opendasharchive.openarchive.util.extensions.executeAsyncTaskWithList
import org.witness.proofmode.crypto.HashUtils
import java.io.File
import java.io.FileNotFoundException
import java.util.*

class MainActivity : AppCompatActivity(), OnTabSelectedListener {

    private val TAG = "OASAVE:Main"
    private var lastTab = 0

    private var mMenuUpload: MenuItem? = null

    private var mSpace: Space? = null
    private var mSnackBar: Snackbar? = null

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mPagerAdapter: ProjectAdapter

    val REQUEST_NEW_PROJECT_NAME = 1001

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            Log.d("receiver", "Updating media")
            val mediaId = intent.getLongExtra(SiteController.MESSAGE_KEY_MEDIA_ID, -1)
            val progress = intent.getLongExtra(SiteController.MESSAGE_KEY_PROGRESS, -1)
            val status = intent.getIntExtra(SiteController.MESSAGE_KEY_STATUS, -1)
            if (status == Media.STATUS_UPLOADED) {
                mBinding.pager.let {
                    if (mBinding.pager.currentItem > 0) {
                        val frag =
                            mPagerAdapter.getRegisteredFragment(mBinding.pager.currentItem) as MediaListFragment
                        frag.refresh()
                        updateMenu()
                    }
                }
            } else if (status == Media.STATUS_UPLOADING) {
                mBinding.pager.let {
                    if (mediaId != -1L && mBinding.pager.currentItem > 0) {
                        val frag =
                            mPagerAdapter.getRegisteredFragment(mBinding.pager.currentItem) as MediaListFragment
                        frag.updateItem(mediaId, progress)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initLayout()
    }

    private fun initLayout() {

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        mBinding.spaceAvatar.setOnClickListener {
            showSpaceSettings()
        }

        mPagerAdapter = ProjectAdapter(this, supportFragmentManager)

        mSnackBar =
            mBinding.pager.createSnackBar(getString(R.string.importing_media), Snackbar.LENGTH_INDEFINITE)
        val snackView = mSnackBar?.view as? SnackbarLayout
        snackView?.addView(ProgressBar(this))

        mSpace?.let {
            val listProjects = getAllBySpace(it.id, false)
            mPagerAdapter.updateData(listProjects)
            mBinding.pager.adapter = mPagerAdapter
            if (!listProjects.isNullOrEmpty()) mBinding.pager.currentItem = 1 else mBinding.pager.currentItem = 0
            mBinding.tabs.removeOnTabSelectedListener(this)
        } ?: run {
            mBinding.pager.adapter = mPagerAdapter
            mBinding.pager.currentItem = 0
            mBinding.tabs.addOnTabSelectedListener(this)
        }


        // final int pageMargin = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 8, getResources() .getDisplayMetrics());
        mBinding.pager.pageMargin = 0

        mBinding.floatingMenu.setOnClickListener {
            if (mPagerAdapter.count > 1 && lastTab > 0)
                importMedia()
            else
                promptAddProject()

        }

        mBinding.pager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {}

            override fun onPageSelected(position: Int) {
                lastTab = position
                if (position == 0) promptAddProject()
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        mBinding.tabs.setupWithViewPager(mBinding.pager)

        //check for any queued uploads and restart
        (application as OpenArchiveApp).uploadQueue()

    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        //NO-Op
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        //No-Op
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
        if (mSpace == null || mPagerAdapter.count <= 1) promptAddProject()
    }

    fun promptAddProject() {
        startActivityForResult(
            Intent(this, AddProjectActivity::class.java),
            REQUEST_NEW_PROJECT_NAME
        )
    }

    fun onNewProjectClicked(view: View?) {
        promptAddProject()
    }

    private fun refreshProjects() {
        mSpace?.let {
            val listProjects = getAllBySpace(it.id, false)
            mPagerAdapter = ProjectAdapter(this, supportFragmentManager)
            mPagerAdapter.updateData(listProjects)
            mBinding.pager.adapter = mPagerAdapter
            if (!listProjects.isNullOrEmpty()) mBinding.pager.currentItem = 1 else mBinding.pager.currentItem = 0
        }
        updateMenu()
    }

    private fun refreshCurrentProject() {
        mBinding.pager.let {
            if (mBinding.pager.currentItem > 0) {
                val frag = mPagerAdapter.getRegisteredFragment(mBinding.pager.currentItem) as? MediaListFragment
                frag?.refresh()
            }
            updateMenu()
        }
    }

    private fun setTitle(title: String) {
        mBinding.spaceName.text = title
    }

    private fun initSpace(space: Space) {
        mSpace = space
        mSpace?.let {
            if (it.name.isNotEmpty()) {
                setTitle(it.name)
            } else {
                val listSpaces = getAllAsList()
                if (listSpaces?.hasNext() == true) {
                    mSpace = listSpaces.next()
                    setTitle(it.name)
                    Prefs.setCurrentSpaceId(it.id)
                } else {
                    setTitle(R.string.main_activity_title)
                }
            }

            if (it.type == Space.TYPE_INTERNET_ARCHIVE) {
                mBinding.spaceAvatar.setImageResource(R.drawable.ialogo128)
            } else {
                val drawable = TextDrawable.builder()
                    .buildRound(
                        it.name.substring(0, 1).toUpperCase(),
                        resources.getColor(R.color.oablue)
                    )
                mBinding.spaceAvatar.setImageDrawable(drawable)
            }
        }
    }

    private fun importSharedMedia(data: Intent?) {
        data?.let {
            scope.executeAsyncTask(
                onPreExecute = {
                    mSnackBar?.show()
                },
                doInBackground = {
                    handleOutsideMedia(data)
                },
                onPostExecute = { media ->
                    if (media != null) {
                        val reviewMediaIntent =
                            Intent(this, ReviewMediaActivity::class.java)
                        reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, media.id)
                        startActivity(reviewMediaIntent)
                    }
                    mSnackBar?.dismiss()
                    intent = null
                }
            )
        }
    }

    private fun updateMenu() {
        mMenuUpload?.let {
            val mStatuses = longArrayOf(
                Media.STATUS_UPLOADING.toLong(),
                Media.STATUS_QUEUED.toLong(), Media.STATUS_ERROR.toLong()
            )
            val uploadCount = getMediaByStatus(mStatuses, Media.ORDER_PRIORITY)?.size ?: 0
            if (uploadCount > 0) {
                it.isVisible = true
                val bg = BadgeDrawable(this)
                bg.setCount("$uploadCount")
                it.icon = bg
            } else {
                it.isVisible = false
            }
        }
    }

    private fun showSpaceSettings() {
        val intent = Intent(this, SpaceSettingsActivity::class.java)
        startActivity(intent)
    }

    private fun importMedia(importUri: List<Uri>): ArrayList<Media> {
        val result = ArrayList<Media>()
        for (uri in importUri) {
            val media = importMedia(uri)
            if (media != null) result.add(media)
        }
        return result
    }

    private fun importMedia(uri: Uri?): Media? {
        if (uri == null) return null
        val title = Utility.getUriDisplayName(this, uri) ?: Constants.EMPTY_STRING
        val mimeType = Utility.getMimeType(this, uri)
        val fileImport = Utility.getOutputMediaFileByCache(this, title)
        try {
            val imported = Utility.writeStreamToFile(
                contentResolver.openInputStream(uri), fileImport
            )
            if (!imported) return null
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return null
        }
        val project = mPagerAdapter.getProject(lastTab) ?: return null

        // create media
        val media = Media()
        var coll: Collection?
        if (project.openCollectionId == -1L) {
            coll = Collection()
            coll.projectId = project.id
            coll.save()
            project.openCollectionId = coll.id
            project.save()
        } else {
            coll = findById<Collection>(Collection::class.java, project.openCollectionId)
            if (coll == null || coll.uploadDate != null) {
                coll = Collection()
                coll.projectId = project.id
                coll.save()
                project.openCollectionId = coll.id
                project.save()
            }
        }
        media.collectionId = coll.id
        val fileSource = File(FileUtils.getMediaPathFromUri(uri, this))
        var createDate = Date()
        if (fileSource.exists()) {
            createDate = Date(fileSource.lastModified())
            media.contentLength = fileSource.length()
        } else media.contentLength = fileImport?.length() ?: 0
        media.originalFilePath = Uri.fromFile(fileImport).toString()
        media.mimeType = mimeType ?: Constants.EMPTY_STRING
        media.createDate = createDate
        media.updateDate = media.createDate
        media.status = Media.STATUS_LOCAL
        media.mediaHashString = HashUtils.getSHA256FromFileContent(fileImport)
        media.projectId = project.id
        if (title != null) media.title = title
        media.save()
        return media
    }

    private fun handleOutsideMedia(intent: Intent?): Media? {
        var media: Media? = null
        if (intent != null && intent.action != null && intent.action == Intent.ACTION_SEND) {
            val mimeType = intent.type
            var uri = intent.data
            if (uri == null) {
                uri =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && intent.clipData != null && intent.clipData!!.itemCount > 0) {
                        intent.clipData!!.getItemAt(0).uri
                    } else {
                        return null
                    }
            }
            media = importMedia(uri)
        }
        return media
    }


    private fun importMedia() {
        if (!askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 1)) {
            Matisse.from(this)
                .choose(MimeType.ofAll(), false)
                .countable(true)
                .maxSelectable(100)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.85f)
                .imageEngine(PicassoEngine())
                .capture(true)
                .captureStrategy(CaptureStrategy(true, "$packageName.provider", "capture"))
                .forResult(Globals.REQUEST_FILE_IMPORT)
        }
    }


    private fun askForPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    permission
                )
            ) {
                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    requestCode
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    requestCode
                )
            }
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 2)
            2 -> importMedia()
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver,
            IntentFilter(INTENT_FILTER_NAME)
        )
        mBinding.spaceAvatar.setImageResource(R.drawable.avatar_default)
        val spaceCurrent = getCurrentSpace()
        spaceCurrent?.let { currentSpace ->
            mSpace?.let {
                val listProjects = getAllBySpace(it.id, false)
                if (it.id != currentSpace.id) {
                    initSpace(spaceCurrent)
                    refreshProjects()
                } else if (listProjects?.size != mPagerAdapter.count - 1) {
                    initSpace(spaceCurrent)
                    refreshProjects()
                } else {
                    initSpace(spaceCurrent)
                }
            } ?: run {
                initSpace(spaceCurrent)
                refreshProjects()
            }
            refreshCurrentProject()
        }
        if (mSpace == null || TextUtils.isEmpty(mSpace?.host)) {
            val intent = Intent(this, OAAppIntro::class.java)
            startActivity(intent)
        }
        val data = intent
        importSharedMedia(data)
        if (mBinding.pager.currentItem == 0 && mPagerAdapter.count > 1) {
            mBinding.pager.currentItem = 1
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        importSharedMedia(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        mMenuUpload = menu.findItem(R.id.menu_upload_manager)
        updateMenu()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            android.R.id.home -> {
                showSpaceSettings()
                return true
            }
            R.id.menu_upload_manager -> {
                startActivity(Intent(this, UploadManagerActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        Log.d(TAG, "onActivityResult, requestCode:$requestCode, resultCode: $resultCode")

        // Check which request we're responding to
        if (requestCode == Globals.REQUEST_FILE_IMPORT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK && resultData != null) {
                val mSelected = Matisse.obtainResult(resultData)

                val bar = mBinding.pager.createSnackBar(
                    getString(R.string.importing_media),
                    Snackbar.LENGTH_INDEFINITE
                )
                val snack_view = bar?.view as SnackbarLayout
                snack_view.addView(ProgressBar(this))

                scope.executeAsyncTaskWithList(
                    onPreExecute = {
                        bar.show()
                    },
                    doInBackground = {
                        importMedia(mSelected)
                    },
                    onPostExecute = { media ->
                        bar.dismiss()
                        refreshCurrentProject()
                        if (media.isNotEmpty()) startActivity(
                            Intent(
                                this,
                                PreviewMediaListActivity::class.java
                            )
                        )
                    }
                )
            }
        } else if (requestCode == REQUEST_NEW_PROJECT_NAME) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                refreshProjects()
            }
        }
    }
    companion object {
        const val INTENT_FILTER_NAME = "MEDIA_UPDATED"
    }

}