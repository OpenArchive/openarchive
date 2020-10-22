package net.opendasharchive.openarchive.media;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.derlio.waveform.SimpleWaveformView;
import com.github.derlio.waveform.soundfile.SoundFile;
import com.squareup.picasso.Picasso;
import com.stfalcon.frescoimageviewer.ImageViewer;

import net.opendasharchive.openarchive.BuildConfig;
import net.opendasharchive.openarchive.OpenArchiveApp;
import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.fragments.MediaViewHolder;
import net.opendasharchive.openarchive.fragments.VideoRequestHandler;
import net.opendasharchive.openarchive.onboarding.SpaceSetupActivity;
import net.opendasharchive.openarchive.publish.PublishService;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.util.Prefs;
import net.opendasharchive.openarchive.util.Utility;

import java.io.File;
import java.util.ArrayList;

import static net.opendasharchive.openarchive.MainActivity.INTENT_FILTER_NAME;


public class ReviewMediaActivity extends AppCompatActivity {
    private static String TAG = "ReviewMediaActivity";

    private Context mContext = this;
    private Media mMedia;
    private long currentMediaId;
    private ProgressDialog progressDialog = null;

    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvAuthor;
    private TextView tvTags;
    private TextView tvLocation;
    private TextView tvUrl;
    private TextView tvLicenseUrl;
    private TextView tvFlagged;


    private SimpleWaveformView swMedia;
    private ImageView ivMedia;

    private SwitchCompat tbDeriv, tbShare, tbComm;

    private MenuItem menuShare;
    private MenuItem menuPublish;


    private ImageView ivEditTags, ivEditLocation, ivEditNotes, ivEditFlag;

    private static Picasso mPicasso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_media);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
     //   actionbar.setHomeAsUpIndicator(R.drawable.arrow_back);

        if (mPicasso == null) {
            VideoRequestHandler videoRequestHandler = new VideoRequestHandler(mContext);

            mPicasso = new Picasso.Builder(mContext)
                    .addRequestHandler(videoRequestHandler)
                    .build();
        }

        // instantiate values
        swMedia = findViewById(R.id.swMedia);

        // display media preview if available
        ivMedia = (ImageView) findViewById(R.id.ivMedia);

        tvTitle = (TextView) findViewById(R.id.tv_title_lbl);
        tvDescription = (TextView) findViewById(R.id.tv_description_lbl);
        tvAuthor = (TextView) findViewById(R.id.tv_author_lbl);
        tvTags = (TextView) findViewById(R.id.tv_tags_lbl);
        tvLocation = (TextView) findViewById(R.id.tv_location_lbl);
        tvUrl = (TextView) findViewById(R.id.tv_url);
        tvFlagged = findViewById(R.id.tv_flag_lbl);


        ivEditTags = findViewById(R.id.ivEditTags);
        ivEditNotes = findViewById(R.id.ivEditNotes);
        ivEditLocation = findViewById(R.id.ivEditLocation);
        ivEditFlag = findViewById(R.id.ivEditFlag);

        tbDeriv = (SwitchCompat)findViewById(R.id.tb_cc_deriv);
        tbDeriv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setLicense();
            }
        });
        tbShare = (SwitchCompat)findViewById(R.id.tb_cc_sharealike);
        tbShare.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setLicense();
            }
        });
        tbComm = (SwitchCompat)findViewById(R.id.tb_cc_comm);
        tbComm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setLicense();
            }
        });

        tvLicenseUrl = (TextView) findViewById(R.id.tv_cc_license);

        setTitle("");


    }

    private void updateFlagState ()
    {
        if (mMedia.isFlagged())
            ivEditFlag.setImageResource(R.drawable.ic_flag_selected);
        else
            ivEditFlag.setImageResource(R.drawable.ic_flag_unselected);

        if (mMedia.isFlagged())
            tvFlagged.setText(R.string.status_flagged);
        else
            tvFlagged.setText(R.string.hint_flag);

        if ((mMedia.status != Media.STATUS_LOCAL
                && mMedia.status != Media.STATUS_NEW) && (!mMedia.isFlagged()))
        {
            ivEditFlag.setVisibility(View.GONE);
            tvFlagged.setVisibility(View.GONE);
        }
    }

    private void bindMedia ()
    {

        // set values
        tvTitle.setText(mMedia.getTitle());

        if (!TextUtils.isEmpty(mMedia.getDescription())) {
            tvDescription.setText(mMedia.getDescription());
            ivEditNotes.setImageResource(R.drawable.ic_edit_selected);
        }



        if (!TextUtils.isEmpty(mMedia.getLocation())) {
            tvLocation.setText(mMedia.getLocation());
            ivEditLocation.setImageResource(R.drawable.ic_location_selected);
        }

        if (!TextUtils.isEmpty(mMedia.getTags())) {
            tvTags.setText(mMedia.getTags());
            ivEditTags.setImageResource(R.drawable.ic_tag_selected);
        }

        tvAuthor.setText(mMedia.getAuthor());
        tvLicenseUrl.setText(mMedia.getLicenseUrl());

        if (mMedia.status != Media.STATUS_LOCAL
                && mMedia.status != Media.STATUS_NEW)
        {

            if (mMedia.status == Media.STATUS_UPLOADED||mMedia.status == Media.STATUS_PUBLISHED) {
            //    tvUrl.setText(Html.fromHtml(getString(R.string.your_media_is_available) + " <a href=\"" + mMedia.getServerUrl() + "\">" + mMedia.getServerUrl() + "</a>"));
             //   tvUrl.setMovementMethod(LinkMovementMethod.getInstance());
              //  tvUrl.setVisibility(View.VISIBLE);
            }
            else if (mMedia.status == Media.STATUS_QUEUED) {
                tvUrl.setText("Waiting for upload...");
                tvUrl.setVisibility(View.VISIBLE);
            }
            else if (mMedia.status == Media.STATUS_UPLOADING) {
                tvUrl.setText("Uploading now...");
                tvUrl.setVisibility(View.VISIBLE);
            }

            tvLicenseUrl.setMovementMethod(LinkMovementMethod.getInstance());

            tvTitle.setEnabled(false);

            tvDescription.setEnabled(false);
            if (TextUtils.isEmpty(mMedia.getDescription())) {
                ivEditNotes.setVisibility(View.GONE);
                tvDescription.setHint("");
            }

            tvAuthor.setEnabled(false);

            tvLocation.setEnabled(false);
            if (TextUtils.isEmpty(mMedia.getLocation())) {
                ivEditLocation.setVisibility(View.GONE);
                tvLocation.setHint("");
            }

            tvTags.setEnabled(false);
            if (TextUtils.isEmpty(mMedia.getTags())) {
                ivEditTags.setVisibility(View.GONE);
                tvTags.setHint("");
            }

            tvLicenseUrl.setEnabled(false);

            findViewById(R.id.groupLicenseChooser).setVisibility(View.GONE);
        }
        else
        {

            findViewById(R.id.row_flag).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    showFirstTimeFlag();

                    mMedia.setFlagged(!mMedia.isFlagged());

                    updateFlagState();
                }
            });


        }

        if (menuPublish != null) {
            if (mMedia.status == Media.STATUS_LOCAL) {
                menuPublish.setVisible(true);
                menuShare.setVisible(false);

            } else {
                menuShare.setVisible(true);
                menuPublish.setVisible(false);
            }
        }

        updateFlagState ();

    }

    private void showFirstTimeFlag ()
    {

        if ( !Prefs.getBoolean("ft.flag")) {
            AlertDialog.Builder build = new AlertDialog.Builder(ReviewMediaActivity.this, R.style.AlertDialogTheme)
                    .setTitle(R.string.popup_flag_title)
                    .setMessage(R.string.popup_flag_desc);


            build.create().show();

            Prefs.putBoolean("ft.flag",true);
        }
    }

    private void setLicense ()
    {

        Project project = Project.getById(mMedia.getProjectId());
        mMedia.setLicenseUrl(project.getLicenseUrl());
    }

    private void saveMedia ()
    {
        //if deleted
        if (mMedia == null)
            return;

        if (tvTitle.getText().length() > 0)
            mMedia.setTitle(tvTitle.getText().toString());
        else
        {
            //use the file name if the user doesn't set a title
            mMedia.setTitle(new File(mMedia.getOriginalFilePath()).getName());
        }

        mMedia.setDescription(tvDescription.getText().toString());
        mMedia.setAuthor(tvAuthor.getText().toString());
        mMedia.setLocation(tvLocation.getText().toString());
        mMedia.setTags(tvTags.getText().toString());

        setLicense();

        if (mMedia.status == Media.STATUS_NEW)
            mMedia.status = Media.STATUS_LOCAL;

        mMedia.save();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveMedia();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_review_media, menu);

        menuShare = menu.findItem(R.id.menu_item_share);
        menuPublish = menu.findItem(R.id.menu_upload);

        if (mMedia.status != Media.STATUS_UPLOADED)
            menuPublish.setVisible(true);
        else {
            menuShare.setVisible(true);
            menuPublish.setVisible(true);
            menuPublish.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub

//        if (drawerListener.onOptionsItemSelected(item)) {
//            return true;
//        }

        Intent intent = null;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_upload:
                uploadMedia();
                break;
          //  case R.id.menu_item_nearby:
            //    startNearby();
              //  break;
            case R.id.menu_item_share_link:
                shareLink();
                break;

            case R.id.menu_item_open_link:
                openLink();
                break;
          //  case R.id.menu_item_share_media:
            //    shareMedia();
              //  break;

            case R.id.menu_delete:
                deleteMedia();

                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void init() {
        Intent intent = getIntent();

        // get intent extras
        currentMediaId = intent.getLongExtra(Globals.EXTRA_CURRENT_MEDIA_ID, -1);

        // get default metadata sharing values
        SharedPreferences sharedPref = this.getSharedPreferences(Globals.PREF_FILE_KEY, Context.MODE_PRIVATE);

        // check for new file or existing media
        if (currentMediaId >= 0) {
            mMedia = Media.findById(Media.class, currentMediaId);
        } else {
            Utility.toastOnUiThread(this, getString(R.string.error_no_media));
            finish();
            return;
        }

        if (mMedia.getMimeType().startsWith("image")) {

            mPicasso.load(Uri.parse(mMedia.getOriginalFilePath())).fit().centerCrop().into(ivMedia);
        }
        else if (mMedia.getMimeType().startsWith("video")) {

            mPicasso.load(VideoRequestHandler.SCHEME_VIDEO + ":" + mMedia.getOriginalFilePath()).fit().centerCrop().into(ivMedia);
        }
        else if (mMedia.getMimeType().startsWith("audio")) {
            ivMedia.setImageDrawable(getResources().getDrawable(R.drawable.audio_waveform));

            SoundFile soundFile = MediaViewHolder.mSoundFileCache.get( mMedia.getOriginalFilePath());
            if (soundFile != null) {
                swMedia.setAudioFile(soundFile);
                swMedia.setVisibility(View.VISIBLE);
                ivMedia.setVisibility(View.GONE);
            }
        }
        else
            ivMedia.setImageDrawable(getResources().getDrawable(R.drawable.no_thumbnail));

        swMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMedia();
            }
        });

        ivMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMedia();
            }
        });
    }

    private void showMedia ()
    {

        if (mMedia.mimeType.startsWith("image"))
        {
            ArrayList<Uri> list = new ArrayList<>();
            list.add(Uri.parse(mMedia.getOriginalFilePath()));
            new ImageViewer.Builder(this,list)
                    .setStartPosition(0)
                    .show();
        }
        else {
            /**
            Intent viewMediaIntent = new Intent();
            viewMediaIntent.setAction(Intent.ACTION_VIEW);

            Uri uriFile = FileProvider.getUriForFile(this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    FileUtils.getFile(this,Uri.parse(mMedia.getOriginalFilePath())));

            viewMediaIntent.setDataAndType(uriFile, mMedia.getMimeType());
            viewMediaIntent.putExtra(Intent.EXTRA_STREAM,uriFile);
            viewMediaIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(viewMediaIntent);
             **/
        }
    }

    private void uploadMedia ()
    {
        
        Space space = Space.getCurrentSpace();


        // if user doesn't have an account
        if (space != null) {

            //mark queued
            mMedia.status = Media.STATUS_QUEUED;
            saveMedia();
            bindMedia();
            startService(new Intent(this, PublishService.class));
            finish();
        }
        else {
            Intent firstStartIntent = new Intent(this, SpaceSetupActivity.class);
            startActivity(firstStartIntent);
        }


    }

    //share the link to the file on the IA
    private void shareLink ()
    {

        StringBuffer sb = new StringBuffer();
        sb.append("\"").append(mMedia.getTitle()).append("\"").append(' ');
        sb.append(getString(R.string.share_text)).append(' ');
        sb.append(mMedia.getServerUrl());

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mMedia.getTitle());
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
    }

    private void openLink ()
    {
        try {
            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mMedia.getServerUrl()));
            startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No application can handle this request."
                    + " Please install a webbrowser",  Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    //share the link to the file on the IA
    private void shareTorrentLink ()
    {

        StringBuffer sb = new StringBuffer();
        sb.append("\"").append(mMedia.getTitle()).append("\"").append(' ');
        sb.append(getString(R.string.share_torrent_text)).append(' ');

        StringBuffer sbTorrentUrl = new StringBuffer();

        String tagId = Uri.parse(mMedia.getServerUrl()).getLastPathSegment();

        sb.append("https://archive.org/download/");
        sb.append(tagId);
        sb.append("/");
        sb.append(tagId);
        sb.append("_archive.torrent");

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mMedia.getTitle());
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
    }

    //share the link to the file on the IA
    private void shareMedia ()
    {

        StringBuffer sb = new StringBuffer();
        sb.append("\"").append(mMedia.getTitle()).append("\"").append(' ');
        sb.append(getString(R.string.share_media_text)).append(' ');
        sb.append(mMedia.getServerUrl());

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType(mMedia.getMimeType());
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mMedia.getTitle());
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());

        Uri sharedFileUri = null;
        String mediaPath = mMedia.getOriginalFilePath();
        if (mediaPath.startsWith("content:"))
            sharedFileUri = Uri.parse(mediaPath);
        else
            sharedFileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider"
                    , new File(mMedia.getOriginalFilePath()));

        sharingIntent.putExtra(Intent.EXTRA_STREAM, sharedFileUri);

        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
    }





    @Override
    protected void onResume() {
        super.onResume();

        init();
        bindMedia();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(INTENT_FILTER_NAME));
    }



    private void showSuccess ()
    {
        Toast.makeText(this,getString(R.string.upload_success),Toast.LENGTH_SHORT).show();
    }


    public void showError(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if(!isFinishing()){
                    new AlertDialog.Builder(ReviewMediaActivity.this, R.style.AlertDialogTheme)
                            .setTitle("Upload Error")
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ReviewMediaActivity.this.finish();
                                }
                            }).create().show();
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }


    // Our handler for received Intents. This will be called whenever an Intent
// with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Log.d("receiver", "Updating media");
            mMedia = Media.findById(Media.class, mMedia.getId());
            bindMedia();

        }
    };

    private void deleteMedia ()
    {


        AlertDialog.Builder build = new AlertDialog.Builder(ReviewMediaActivity.this, R.style.AlertDialogTheme)
            .setTitle(R.string.popup_remove_title)
            .setMessage(R.string.popup_remove_desc)
            .setCancelable(true).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //do nothing
            }
        })

            .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    deleteMedia (false, false);
                    finish();

                }
            });


        build.create().show();
    }

    private void deleteRemoteAndLocal ()
    {

    }

    private void deleteMedia (boolean deleteLocalFile, boolean deleteRemoteFile)
    {
        if (deleteRemoteFile)
        {
            mMedia.status = Media.STATUS_DELETE_REMOTE;
            mMedia.save();
            //start upload queue, which will also handle the deletes
            ((OpenArchiveApp)getApplication()).uploadQueue();
        }
        else {
            Media media = Media.findById(Media.class, currentMediaId);
            if (media != null) {
                boolean success = Media.findById(Media.class, currentMediaId).delete();
                Log.d("OAMedia", "Item deleted: " + success);
                mMedia = null;
            }
        }
    }
}