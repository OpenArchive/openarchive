package net.opendasharchive.openarchive.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Collection;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.MediaAdapter;
import net.opendasharchive.openarchive.media.PreviewMediaListActivity;

import java.util.HashMap;
import java.util.List;

public class MediaGridFragment extends MediaListFragment {

    private int numberOfColumns = 4;
    private HashMap<Long,MediaAdapter> mAdapters;
    private HashMap<Long,SectionViewHolder> mSection;

    private View mMainView;

    private View mMediaHint;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MediaGridFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mAdapters = new HashMap<>();
        mSection = new HashMap<>();

        mMainView = inflater.inflate(R.layout.fragment_media_list, container, false);
        mMainView.setTag(TAG);

        LinearLayout mainContainer = mMainView.findViewById(R.id.mediacontainer);

        mMediaHint = mMainView.findViewById(R.id.add_media_hint);

        List<Collection> listCollections = Collection.getAllAsList();

        boolean addedView = false;
        for (Collection coll : listCollections) {

            List<Media> listMedia = Media.getMediaByProjectAndCollection(mProjectId, coll.getId());
            if (listMedia.size() > 0)
            {
                if (!addedView)
                {
                    mainContainer.removeAllViews();
                    addedView = true;
                }

                View view = createMediaList(mainContainer, coll, listMedia);
                mainContainer.addView(view);

            }
        }

        if (mMediaHint != null)
            mMediaHint.setVisibility(addedView? View.GONE : View.VISIBLE);

        return mMainView;
    }

    private class SectionViewHolder {
        View mediaSection;
        RecyclerView.Recycler mediaGrid;
        TextView sectionStatus;
        TextView sectionTimestamp;
        View action;
    }

    private View createMediaList (LinearLayout mainContainer, Collection coll, List<Media> listMedia)
    {
        GridLayoutManager lMan = new GridLayoutManager(getActivity(), numberOfColumns) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };

        SectionViewHolder holder = new SectionViewHolder();

        holder.mediaSection = getLayoutInflater().inflate(R.layout.fragment_media_list_section, null);
        RecyclerView rView = holder.mediaSection.findViewById(R.id.recyclerview);
        rView.setHasFixedSize(true);
        rView.setLayoutManager(lMan);

        holder.sectionStatus = holder.mediaSection.findViewById(R.id.sectionstatus);
        holder.sectionTimestamp = holder.mediaSection.findViewById(R.id.sectiontimestamp);
        holder.action = holder.mediaSection.findViewById(R.id.action_next);

        setSectionHeaders(coll, listMedia, holder);

        MediaAdapter mediaAdapter = new MediaAdapter(getActivity(), R.layout.activity_media_list_square, listMedia, rView, new OnStartDragListener() {
            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {

            }
        });
        rView.setAdapter(mediaAdapter);
        mAdapters.put(coll.getId(),mediaAdapter);
        mSection.put(coll.getId(),holder);

        return holder.mediaSection;
    }

    public void updateItem (long mediaId, long progress)
    {
        for (MediaAdapter adapter : mAdapters.values())
            adapter.updateItem(mediaId, progress);

    }

    public void refresh ()
    {

        LinearLayout mainContainer = mMainView.findViewById(R.id.mediacontainer);

        List<Collection> listCollections = Collection.getAllAsList();

        for (Collection coll : listCollections) {

            List<Media> listMedia = Media.getMediaByProjectAndCollection(mProjectId, coll.getId());

            MediaAdapter adapter = mAdapters.get(coll.getId());
            SectionViewHolder holder = mSection.get(coll.getId());

            if (adapter != null)
            {
                adapter.updateData(listMedia);

                setSectionHeaders(coll, listMedia, holder);

            }
            else if (listMedia.size() > 0)
            {
                View view = createMediaList(mainContainer, coll,listMedia);
                mainContainer.addView(view,0);

                if (mMediaHint != null)
                    mMediaHint.setVisibility(View.GONE);
            }
        }

    }

    private void setSectionHeaders (Collection coll, List<Media> listMedia, SectionViewHolder holder)
    {
        for (Media media : listMedia)
        {
            if (media.status == Media.STATUS_LOCAL)
            {
                holder.sectionStatus.setText(R.string.status_ready_to_upload);
                holder.sectionTimestamp.setText(listMedia.size() + getString(R.string.label_items));
                holder.action.setVisibility(View.VISIBLE);
                holder.action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(getActivity(), PreviewMediaListActivity.class));
                    }
                });
                break;

            }
            else if (media.status == Media.STATUS_QUEUED || media.status == Media.STATUS_UPLOADING)
            {
                holder.sectionStatus.setText(R.string.header_uploading);

                int uploadedCount = 0;
                for (Media localMedia : listMedia)
                    if (localMedia.status == Media.STATUS_UPLOADED)
                        uploadedCount++;

                holder.sectionTimestamp.setText(uploadedCount + " " + getString(R.string.label_out_of) + " " + listMedia.size() + ' ' + getString(R.string.label_items_uploaded));


                holder.action.setVisibility(View.GONE);
                break;
            }
            else
            {
                holder.sectionStatus.setText(listMedia.size() + " " + getString(R.string.label_items_uploaded));
                if (coll.getUploadDate() != null)
                    holder.sectionTimestamp.setText(coll.getUploadDate().toLocaleString());
                else if (listMedia.size() > 0 && listMedia.get(0).uploadDate != null)
                    holder.sectionTimestamp.setText(listMedia.get(0).uploadDate.toString());

                holder.action.setVisibility(View.GONE);
            }
        }
    }
}