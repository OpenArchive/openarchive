package net.opendasharchive.openarchive.features.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentMediaListSimpleBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Media.Companion.getMediaByStatus
import net.opendasharchive.openarchive.db.MediaAdapter
import net.opendasharchive.openarchive.features.media.list.MediaListFragment
import java.util.*

class MediaReviewListFragment : MediaListFragment() {

    protected var mStatus = Media.STATUS_LOCAL.toLong()
    protected var mStatuses = longArrayOf(Media.STATUS_LOCAL.toLong())

    private var _mBinding: FragmentMediaListSimpleBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _mBinding = FragmentMediaListSimpleBinding.inflate(layoutInflater, container, false)
        return _mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLayout()
    }

    private fun initLayout() {
        _mBinding?.let { mBinding ->
            mBinding.root.tag = TAG

            mBinding.recyclerview.layoutManager = LinearLayoutManager(activity)
            mBinding.recyclerview.setHasFixedSize(true)

            val listMedia: List<Media>? = getMediaByStatus(mStatuses, Media.ORDER_PRIORITY)

            val listMediaArray = ArrayList(listMedia)
            var mediaAdapter = MediaAdapter(requireActivity(),
                R.layout.activity_media_list_row,
                listMediaArray,
                mBinding.recyclerview,
                object : OnStartDragListener {
                    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder?) {
                        //NO-OP
                    }
                }
            )

            mMediaAdapter = mediaAdapter

            mediaAdapter.setDoImageFade(false)
            mBinding.recyclerview.adapter = mediaAdapter
        }
    }

    override fun setStatus(status: Long) {
        mStatus = status
    }

    override fun refresh() {
        if (mMediaAdapter != null) {
            val listMedia = getMediaByStatus(mStatuses, Media.ORDER_PRIORITY)
            val listMediaArray = ArrayList(listMedia)
            mMediaAdapter?.updateData(listMediaArray)
        }
    }

}