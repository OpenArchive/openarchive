package net.opendasharchive.openarchive.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.github.derlio.waveform.SimpleWaveformView;
import com.github.derlio.waveform.soundfile.SoundFile;
import com.squareup.picasso.Picasso;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.util.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.HashMap;

public class MediaViewHolder extends RecyclerView.ViewHolder {

    private View mView;
    private ImageView ivIcon;
    private TextView tvTitle;
    private TextView tvCreateDate;
    private SimpleWaveformView tvWave;

    private ProgressBar progressBar;
    private TextView tvProgress;

    private Context mContext;

    private static Picasso mPicasso;
    private AsyncTask<Void, Void, SoundFile> asyncTask;
    public static HashMap<String,SoundFile> mSoundFileCache = new HashMap<>();

    public boolean doImageFade = true;
    private String lastMediaPath = null;

    public ImageView ivEditTags, ivEditLocation, ivEditNotes, ivEditFlag, ivIsVideo;
    public final ImageView handleView;

    public MediaViewHolder(final View itemView, Context context) {
        super(itemView);

        mView= itemView;
        mContext = context;

        ivIcon = itemView.findViewById(R.id.ivIcon);
        ivIsVideo = itemView.findViewById(R.id.iconVideo);
        tvTitle = itemView.findViewById(R.id.tvTitle);
        tvCreateDate = itemView.findViewById(R.id.tvCreateDate);
        tvWave = itemView.findViewById(R.id.event_item_sound);

        ivEditTags = itemView.findViewById(R.id.ivEditTags);
        ivEditNotes = itemView.findViewById(R.id.ivEditNotes);
        ivEditLocation = itemView.findViewById(R.id.ivEditLocation);
        ivEditFlag = itemView.findViewById(R.id.ivEditFlag);

        tvProgress = itemView.findViewById(R.id.txtProgress);
        progressBar = itemView.findViewById(R.id.progressBar);

        if (mPicasso == null) {
            VideoRequestHandler videoRequestHandler = new VideoRequestHandler(mContext);

            mPicasso = new Picasso.Builder(mContext)
                    .addRequestHandler(videoRequestHandler)
                    .build();
        }

        handleView = (ImageView) itemView.findViewById(R.id.handle);

    }

    public void bindData(final Media currentMedia, boolean isBatchMode) {

        mView.setTag(currentMedia.getId());

        if (currentMedia.isSelected() && isBatchMode)
        {
            mView.setBackgroundResource(R.color.oablue);
        }
        else
        {
            mView.setBackgroundResource(android.R.color.transparent);
        }

        final String mediaPath = currentMedia.getOriginalFilePath();

        if (currentMedia.status == Media.STATUS_PUBLISHED || currentMedia.status == Media.STATUS_UPLOADED) {
            ivIcon.setAlpha(1f);
        }
        else
        {
            if (doImageFade)
                ivIcon.setAlpha(0.5f);
            else
                ivIcon.setAlpha(1f);
        }

        if (lastMediaPath == null || (!lastMediaPath.equals(mediaPath)))
        {
            if (currentMedia.getMimeType().startsWith("image")) {

                mPicasso.load(Uri.parse(currentMedia.getOriginalFilePath())).fit().centerCrop().into(ivIcon);
                ivIcon.setVisibility(View.VISIBLE);
                tvWave.setVisibility(View.GONE);

                if (ivIsVideo != null)
                    ivIsVideo.setVisibility(View.GONE);

            } else if (currentMedia.getMimeType().startsWith("video")) {

                mPicasso.load(VideoRequestHandler.SCHEME_VIDEO + ":" + currentMedia.getOriginalFilePath()).fit().centerCrop().into(ivIcon);
                ivIcon.setVisibility(View.VISIBLE);
                tvWave.setVisibility(View.GONE);

                if (ivIsVideo != null)
                    ivIsVideo.setVisibility(View.VISIBLE);

            } else if (currentMedia.getMimeType().startsWith("audio")) {

                ivIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.no_thumbnail));


                if (ivIsVideo != null)
                    ivIsVideo.setVisibility(View.GONE);

                if (mSoundFileCache.get(mediaPath) == null) {

                    if (asyncTask == null) {
                        asyncTask = new AsyncTask<Void, Void, SoundFile>() {
                            protected void onPreExecute() {
                                // Pre Code
                            }

                            protected SoundFile doInBackground(Void... unused) {
                                File fileSound = FileUtils.getFile(mContext, Uri.parse(mediaPath));
                                try {
                                    if (fileSound != null) {
                                        final SoundFile soundFile = SoundFile.create(fileSound.getPath(), new SoundFile.ProgressListener() {
                                            int lastProgress = 0;

                                            @Override
                                            public boolean reportProgress(double fractionComplete) {
                                                final int progress = (int) (fractionComplete * 100);
                                                if (lastProgress == progress) {
                                                    return true;
                                                }
                                                lastProgress = progress;
                                                return true;
                                            }
                                        });

                                        mSoundFileCache.put(mediaPath, soundFile);
                                        return soundFile;
                                    } else
                                        return null;

                                } catch (Exception e) {
                                    Log.e(getClass().getName(), "error loading sound file", e);
                                }

                                return null;
                            }

                            protected void onPostExecute(SoundFile soundFile) {
                                // Post Code

                                if (soundFile != null) {
                                    tvWave.setAudioFile(soundFile);
                                    tvWave.setVisibility(View.VISIBLE);
                                    ivIcon.setVisibility(View.GONE);
                                }

                                asyncTask = null;
                            }
                        };

                        asyncTask.execute();
                    }
                } else {
                    tvWave.setAudioFile(mSoundFileCache.get(mediaPath));
                    tvWave.setVisibility(View.VISIBLE);
                    ivIcon.setVisibility(View.GONE);
                }

            } else
                ivIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.no_thumbnail));


            File fileMedia = new File(Uri.parse(currentMedia.getOriginalFilePath()).getPath());

            if (fileMedia.exists())
            {
                tvCreateDate.setText(readableFileSize(fileMedia.length()));
            }
            else {

                if (currentMedia.contentLength == -1)
                {
                    try {
                        InputStream is = mContext.getContentResolver().openInputStream(Uri.parse(currentMedia.getOriginalFilePath()));
                        currentMedia.contentLength = is.available();
                        currentMedia.save();
                        is.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                if (currentMedia.contentLength > 0)
                    tvCreateDate.setText(readableFileSize(currentMedia.contentLength));
                else
                    tvCreateDate.setText(currentMedia.getFormattedCreateDate());
            }
        }

        lastMediaPath = mediaPath;

        StringBuffer sbTitle = new StringBuffer();

        if (currentMedia.status == Media.STATUS_ERROR) {
            sbTitle.append(mContext.getString(R.string.status_error));

            if (progressBar != null)
            {
                progressBar.setVisibility(View.GONE);
                tvProgress.setVisibility(View.GONE);
                progressBar.setProgress(0);
                tvProgress.setText(0 + "%");
            }

            if (!TextUtils.isEmpty(currentMedia.statusMessage))
                tvCreateDate.setText(currentMedia.statusMessage);

        }
        else if (currentMedia.status == Media.STATUS_QUEUED) {
            sbTitle.append(mContext.getString(R.string.status_waiting));

            if (progressBar != null)
            {
                progressBar.setVisibility(View.VISIBLE);
                tvProgress.setVisibility(View.VISIBLE);

                progressBar.setProgress(0);
                tvProgress.setText(0 + "%");
            }

        } else if (currentMedia.status == Media.STATUS_UPLOADING || currentMedia.status == Media.STATUS_UPLOADED) {
            sbTitle.append(mContext.getString(R.string.status_uploading));

             int perc = 0;

             if (currentMedia.contentLength > 0)
                perc = (int)(((float)currentMedia.progress) / ((float)currentMedia.contentLength) * 100f);

             if (progressBar != null)
             {
                 progressBar.setVisibility(View.VISIBLE);
                 tvProgress.setVisibility(View.VISIBLE);
                 progressBar.setProgress(perc);
                 tvProgress.setText(perc + "%");
             }
             else {
                 sbTitle.append(" ").append(perc + "%");
             }
        }

        if (sbTitle.length() > 0)
            sbTitle.append(": ");

        sbTitle.append(currentMedia.getTitle());
        tvTitle.setText(sbTitle.toString());

        if (ivEditLocation != null)
            if (!TextUtils.isEmpty(currentMedia.getLocation()))
                ivEditLocation.setImageResource(R.drawable.ic_location_selected);
            else
                ivEditLocation.setImageResource(R.drawable.ic_location_unselected);

        if (ivEditTags != null)
            if (!TextUtils.isEmpty(currentMedia.getTags()))
                ivEditTags.setImageResource(R.drawable.ic_tag_selected);
            else
                ivEditTags.setImageResource(R.drawable.ic_tag_unselected);

        if (ivEditNotes != null)
            if (!TextUtils.isEmpty(currentMedia.getDescription()))
                ivEditNotes.setImageResource(R.drawable.ic_edit_selected);
            else
                ivEditNotes.setImageResource(R.drawable.ic_edit_unselected);

        if (ivEditFlag != null)
        if (currentMedia.isFlagged())
            ivEditFlag.setImageResource(R.drawable.ic_flag_selected);
        else
            ivEditFlag.setImageResource(R.drawable.ic_flag_unselected);
    }

    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
