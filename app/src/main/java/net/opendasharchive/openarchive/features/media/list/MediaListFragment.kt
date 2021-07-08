package net.opendasharchive.openarchive.features.media.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentMediaListBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Media.Companion.getMediaByProject
import net.opendasharchive.openarchive.db.Media.Companion.getMediaByStatus
import net.opendasharchive.openarchive.db.MediaAdapter
import net.opendasharchive.openarchive.util.Constants.EMPTY_ID

open class MediaListFragment : Fragment() {

    private var mProjectId: Long = -1
    private var mStatus = Media.STATUS_UPLOADING.toLong()
    private var mStatuses = longArrayOf(
        Media.STATUS_UPLOADING.toLong(),
        Media.STATUS_QUEUED.toLong(), Media.STATUS_ERROR.toLong()
    )
    open var mMediaAdapter: MediaAdapter? = null

    private var _mBinding: FragmentMediaListBinding? = null
    private lateinit var viewModel: MediaListViewModel

    private val mItemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.END or ItemTouchHelper.START
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            mMediaAdapter?.onItemMove(
                viewHolder.adapterPosition,
                target.adapterPosition
            )
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            mMediaAdapter?.onItemDismiss(viewHolder.adapterPosition)
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _mBinding = FragmentMediaListBinding.inflate(inflater, container, false)
        _mBinding?.root?.tag = TAG
        viewModel = ViewModelProvider(this).get(MediaListViewModel::class.java)
        observeData()
        viewModel.getMediaList(mProjectId, mStatuses)
        return _mBinding?.root
    }

    private fun observeData() {
        viewModel.mediaList.observe(viewLifecycleOwner, Observer {
            if (mProjectId != EMPTY_ID) {
                it?.forEach { media ->
                    if (media.status == Media.STATUS_LOCAL) {
                        return@forEach
                    }
                }
                initLayout(it ?: listOf())
            }
        })
    }

    private fun initLayout(mediaList: List<Media>) {
        val rView = RecyclerView(requireContext())
        rView.layoutManager = LinearLayoutManager(activity)
        rView.setHasFixedSize(true)
        _mBinding?.mediacontainer?.addView(rView)

        val listMediaArray = ArrayList(mediaList)

        mMediaAdapter =
            MediaAdapter(
                requireContext(),
                R.layout.activity_media_list_row_short,
                listMediaArray,
                rView,
                object : OnStartDragListener {
                    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder?) {
                        if (viewHolder != null) mItemTouchHelper.startDrag(viewHolder)
                    }

                })
        mMediaAdapter?.setDoImageFade(false)
        rView.adapter = mMediaAdapter
        mItemTouchHelper.attachToRecyclerView(rView)
    }

    fun getMediaList(): List<Media>? {
        return mMediaAdapter?.getMediaList()
    }

    open fun setStatus(status: Long) {
        mStatus = status
    }

    open fun getProjectId(): Long {
        return mProjectId
    }

    fun setProjectId(projectId: Long) {
        mProjectId = projectId
    }

    open fun updateItem(mediaId: Long, progress: Long) {
        mMediaAdapter?.updateItem(mediaId, progress)
    }

    fun stopBatchMode() {
        mMediaAdapter?.getActionMode()?.finish()
    }

    fun setEditMode(isEditMode: Boolean) {
        mMediaAdapter?.setEditMode(isEditMode)
    }

    open fun refresh() {
        var listMedia: List<Media>? = if (mProjectId == -1L) {
            getMediaByStatus(mStatuses, Media.ORDER_PRIORITY)
        } else {
            getMediaByProject(mProjectId)
        }
        listMedia?.let {
            val listMediaArray = ArrayList(it)
            mMediaAdapter?.updateData(listMediaArray)
        } ?: run {
            mMediaAdapter?.updateData(arrayListOf())
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    interface OnStartDragListener {
        /**
         * Called when a view is requesting a start of a drag.
         *
         * @param viewHolder The holder of the view to drag.
         */
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder?)
    }

    companion object {
        const val TAG = "RecyclerViewFragment"
    }
}