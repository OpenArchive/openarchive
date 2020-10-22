package net.opendasharchive.openarchive.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.MediaAdapter;

import java.util.List;

public class MediaListFragment extends Fragment {


    protected LinearLayout mMediaContainer;
    MediaAdapter mMediaAdapter;
    protected static final String TAG = "RecyclerViewFragment";

    protected long mProjectId = -1;
    protected long mStatus = Media.STATUS_UPLOADING;
    protected long[] mStatuses = {Media.STATUS_UPLOADING,Media.STATUS_QUEUED, Media.STATUS_ERROR};


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MediaListFragment() {
    }

    public List<Media> getMediaList ()
    {
        return mMediaAdapter.getMediaList();
    }

    public void setStatus (long status) {
        mStatus = status;
    }

    public void setProjectId (long projectId)
    {
        mProjectId = projectId;
    }

    public void updateItem (long mediaId, long progress)
    {
        if (mMediaAdapter != null)
            mMediaAdapter.updateItem(mediaId, progress);

    }

    public void stopBatchMode ()
    {
        if (mMediaAdapter != null && mMediaAdapter.getActionMode() != null)
            mMediaAdapter.getActionMode().finish();
    }

    public void refresh ()
    {
        if (mMediaAdapter != null)
        {
            List<Media> listMedia = null;

            if (mProjectId == -1)
            {
                listMedia = Media.getMediaByStatus(mStatuses, Media.ORDER_PRIORITY);

            }
            else
            {
                listMedia = Media.getMediaByProject(mProjectId);
            }

            mMediaAdapter.updateData(listMedia);

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_media_list, container, false);
        rootView.setTag(TAG);

        mMediaContainer = rootView.findViewById(R.id.mediacontainer);

        //mRecyclerView = rootView.findViewById(R.id.recyclerview);
        RecyclerView rView = new RecyclerView(getContext());
        rView.setLayoutManager(new LinearLayoutManager(getActivity()));
        rView.setHasFixedSize(true);

        mMediaContainer.addView(rView);

        List<Media> listMedia = null;

        if (mProjectId == -1)
        {
            listMedia = Media.getMediaByStatus(mStatuses, Media.ORDER_PRIORITY);
        }
        else
        {
            listMedia = Media.getMediaByProject(mProjectId);

            for (Media media : listMedia)
            {
                if (media.status == Media.STATUS_LOCAL)
                {

                    break;
                }
            }
        }

        mMediaAdapter = new MediaAdapter(getActivity(), R.layout.activity_media_list_row_short, listMedia, rView, new OnStartDragListener() {
            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {

                mItemTouchHelper.startDrag(viewHolder);

            }
        });
        mMediaAdapter.setDoImageFade(false);
        rView.setAdapter(mMediaAdapter);
        mItemTouchHelper.attachToRecyclerView(rView);

        return rootView;
    }

    private ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP|ItemTouchHelper.DOWN,ItemTouchHelper.END|ItemTouchHelper.START) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {

            mMediaAdapter.onItemMove(viewHolder.getAdapterPosition(),
                    target.getAdapterPosition());

            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            mMediaAdapter.onItemDismiss(viewHolder.getAdapterPosition());
        }
    });

    public void setEditMode (boolean isEditMode)
    {
        mMediaAdapter.setEditMode(isEditMode);
    }

    public interface OnStartDragListener {

        /**
         * Called when a view is requesting a start of a drag.
         *
         * @param viewHolder The holder of the view to drag.
         */
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

}