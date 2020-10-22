package net.opendasharchive.openarchive.db;


import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.media.ReviewMediaActivity;
import net.opendasharchive.openarchive.fragments.MediaViewHolder;
import net.opendasharchive.openarchive.util.Globals;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.Section;

public class MediaSection extends Section {

    private List<Media> mMediaList;
    private String mTitle;
    private Context mContext;
    private RecyclerView mRecyclerView;

    public MediaSection(Context context, RecyclerView recyclerView, int mediaLayoutId, String title, List<Media> mediaList) {
        super(SectionParameters.builder()
                .itemResourceId(mediaLayoutId)
                .headerResourceId(R.layout.media_section_header)
                .build());
        mTitle = title;
        mMediaList = mediaList;
        mContext = context;
        mRecyclerView = recyclerView;

        setHasHeader(true);

    }

    @Override
    public int getContentItemsTotal() {
        return mMediaList.size(); // number of items of this section
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                long mediaId = (Long)view.getTag();

                Intent reviewMediaIntent = new Intent(mContext, ReviewMediaActivity.class);
                reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, mediaId);
                reviewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mContext.startActivity(reviewMediaIntent);
            }
        });
        return new MediaViewHolder(view, mContext);
    }

    @Override
    public void onBindItemViewHolder(androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
        MediaViewHolder itemHolder = (MediaViewHolder) holder;
        itemHolder.bindData(mMediaList.get(position),true);

    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new HeaderViewHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
        HeaderViewHolder headerHolder = (HeaderViewHolder) holder;

        headerHolder.tvTitle.setText(mTitle);
    }

    public void updateData (List<Media> mediaList)
    {
        this.mMediaList = mediaList;
        notify();
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;

        HeaderViewHolder(View view) {
            super(view);

            tvTitle = view.findViewById(R.id.tvSectionTitle);
        }
    }
}
