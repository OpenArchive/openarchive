package net.opendasharchive.openarchive.db;

import android.content.Context;
import android.content.Intent;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.fragments.MediaListFragment;
import net.opendasharchive.openarchive.fragments.MediaViewHolder;
import net.opendasharchive.openarchive.media.BatchReviewMediaActivity;
import net.opendasharchive.openarchive.media.ReviewMediaActivity;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.util.Prefs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by micahjlucas on 1/20/15.
 */
public class MediaAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private int layoutResourceId;
    private List<Media> data;
    private RecyclerView recyclerview;
    private boolean doImageFade = true;
    private final MediaListFragment.OnStartDragListener mDragStartListener;
    private boolean isEditMode = false;
    private ActionMode mActionMode;

    public MediaAdapter(Context context, int layoutResourceId, List<Media> data, RecyclerView recyclerView, MediaListFragment.OnStartDragListener dragStartListener) {
        super();
        this.layoutResourceId = layoutResourceId;
        this.mContext = context;
        this.data = data;
        this.recyclerview = recyclerView;
        this.mDragStartListener = dragStartListener;
    }

    public void setDoImageFade (boolean doImageFade)
    {
        this.doImageFade = doImageFade;
    }

    public List<Media> getMediaList ()
    {
        return data;
    }

    public boolean updateItem (long mediaId, long progress)
    {
        for (int i = 0; i < data.size(); i++)
        {
            Media item = data.get(i);
            if (item.getId() == mediaId)
            {
                item.status = Media.STATUS_UPLOADING;
                item.progress = progress;
                notifyItemChanged(i);

               return true;
            }
        }

        return false;
    }

    public void updateData (List<Media> data)
    {
        this.data = data;

        int priority = data.size();

        for (Media media: data)
        {
            media.setPriority(priority--);
            media.save();
        }

        notifyDataSetChanged();

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public ActionMode getActionMode ()
    {
        return mActionMode;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        final View view = LayoutInflater.from(parent.getContext()).inflate(layoutResourceId, parent, false);

        MediaViewHolder mvh = new MediaViewHolder(view, mContext);
        mvh.doImageFade = doImageFade;

        view.setOnClickListener(view1 -> {

            if (mActionMode != null)
            {
                selectView(view1);
            }
            else {
                int itemPosition = recyclerview.getChildLayoutPosition(view1);

                Intent reviewMediaIntent = new Intent(mContext, ReviewMediaActivity.class);
                reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, data.get(itemPosition).getId());
                reviewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mContext.startActivity(reviewMediaIntent);
            }
        });

        view.setOnLongClickListener(v -> {
            if (mActionMode != null) {
                return false;
            }


            // Start the CAB using the ActionMode.Callback defined above
            mActionMode = ((AppCompatActivity) mContext).startActionMode(mActionModeCallback);
            ((AppCompatActivity) mContext).getSupportActionBar().hide();

            selectView(v);


            return true;
        });

        if (mvh.ivEditFlag != null)
            mvh.ivEditFlag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    showFirstTimeFlag ();

                    //toggle flag
                    long mediaId = (long)view.getTag();

                    for (Media media : getMediaList()) {

                        if (media.getId() == mediaId) {
                            media.setFlagged(!media.isFlagged());
                            media.save();
                            break;
                        }
                    }

                    notifyDataSetChanged();

                }
            });

        return mvh;
    }

    private void showFirstTimeFlag ()
    {

        if ( !Prefs.getBoolean("ft.flag")) {
            AlertDialog.Builder build = new AlertDialog.Builder(mContext, R.style.AlertDialogTheme)
                    .setTitle(R.string.popup_flag_title)
                    .setMessage(R.string.popup_flag_desc);


            build.create().show();

            Prefs.putBoolean("ft.flag",true);
        }
    }

    private void selectView (View view)
    {
        long mediaId = (long)view.getTag();

        for (Media media : getMediaList()) {

            if (media.getId() == mediaId) {
                media.setSelected(!media.isSelected());
                media.save();
                break;
            }
        }

        notifyDataSetChanged();

    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        MediaViewHolder mvh = (MediaViewHolder)holder;

        mvh.bindData(data.get(position),mActionMode != null);

        if (mvh.handleView != null) {
            if (isEditMode)
                mvh.handleView.setVisibility(View.VISIBLE);
            else
                mvh.handleView.setVisibility(View.GONE);

            mvh.handleView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEventCompat.getActionMasked(event) ==
                            MotionEvent.ACTION_DOWN) {
                        if (mDragStartListener != null)
                            mDragStartListener.onStartDrag(holder);
                    }
                    return false;
                }
            });
        }
    }

//    @Override
 //   public int getItemViewType(final int position) {
   //     return layoutResourceId;
   // }

    public void setEditMode (boolean isEditMode)
    {
        this.isEditMode = isEditMode;
    }

    public void onItemMove (int oldPos, int newPos)
    {
        if (this.isEditMode) {
            Media mediaToMov = data.get(oldPos);

            data.remove(oldPos);
            data.add(newPos, mediaToMov);

            int priority = data.size();

            for (Media media : data) {
                media.setPriority(priority--);
                media.save();
            }
        }

        notifyItemMoved(oldPos, newPos);
    }

    public void onItemDismiss (int pos)
    {
        if (this.isEditMode) {
            Media mediaToDismiss = data.get(pos);
            data.remove(mediaToDismiss);
            mediaToDismiss.status = Media.STATUS_LOCAL;
            mediaToDismiss.save();
        }

        notifyDataSetChanged();


    }

    ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_batch_edit_media, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            switch (item.getItemId()) {
                case R.id.menu_edit:

                    List<Media> selectedMedia = Media.find(Media.class, "selected = ?", "1");

                    long[] selectedMediaIds = new long[selectedMedia.size()];
                    for (int i = 0; i < selectedMediaIds.length; i++)
                        selectedMediaIds[i] = selectedMedia.get(i).getId();

                    Intent intent = new Intent(mContext, BatchReviewMediaActivity.class);
                    intent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID,selectedMediaIds);
                    mContext.startActivity(intent);
                    return true;

                case R.id.menu_delete:

                    Iterator<Media> it = new ArrayList<>(data).iterator();
                    while (it.hasNext())
                    {
                        Media mediaDelete = it.next();
                        if (mediaDelete.isSelected()) {
                            data.remove(mediaDelete);
                            mediaDelete.delete();
                        }
                    }

                    mode.finish();
                    notifyDataSetChanged();



                    return true;
                //    deleteMessage(((MessageListItem)mLastSelectedView).getPacketId(),((MessageListItem)mLastSelectedView).getLastMessage());
                  //  mode.finish(); // Action picked, so close the CAB

                default:
                    return false;
            }


        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;


            for (Media media : getMediaList())
            {
                media.setSelected(false);
                media.save();
            }

            notifyDataSetChanged();

            ((AppCompatActivity) mContext).getSupportActionBar().show();

        }


    };
}