package net.opendasharchive.openarchive.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.MediaAdapter;

import java.util.List;

public class MediaReviewListFragment extends MediaListFragment {

    protected long mStatus = Media.STATUS_LOCAL;
    protected long[] mStatuses = {Media.STATUS_LOCAL};

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MediaReviewListFragment() {
    }

    public void setStatus (long status) {
        mStatus = status;
    }

    public void refresh ()
    {
        if (mMediaAdapter != null)
        {
            List<Media> listMedia = Media.getMediaByStatus(mStatuses, Media.ORDER_PRIORITY);
            mMediaAdapter.updateData(listMedia);

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_media_list_simple, container, false);
        rootView.setTag(TAG);

        mMediaContainer = rootView.findViewById(R.id.mediacontainer);

        RecyclerView rView = new RecyclerView(getContext());
        rView.setLayoutManager(new LinearLayoutManager(getActivity()));
        rView.setHasFixedSize(true);

        rView = rootView.findViewById(R.id.recyclerview);
        rView.setLayoutManager(new LinearLayoutManager(getActivity()));
        rView.setHasFixedSize(true);

        List<Media> listMedia = Media.getMediaByStatus(mStatuses, Media.ORDER_PRIORITY);

        mMediaAdapter = new MediaAdapter(getActivity(), R.layout.activity_media_list_row, listMedia, rView, new OnStartDragListener() {
            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {

            }
        });


        mMediaAdapter.setDoImageFade(false);
        rView.setAdapter(mMediaAdapter);

        return rootView;
    }


}