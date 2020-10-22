package net.opendasharchive.openarchive;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.BaseObj;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.amulyakhare.textdrawable.TextDrawable;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.PicassoEngine;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;
import net.opendasharchive.openarchive.core.SpaceSettingsActivity;
import net.opendasharchive.openarchive.db.Collection;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.db.ProjectAdapter;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.fragments.MediaListFragment;
import net.opendasharchive.openarchive.media.PreviewMediaListActivity;
import net.opendasharchive.openarchive.media.ReviewMediaActivity;
import net.opendasharchive.openarchive.onboarding.CustomOnboardingScreen;
import net.opendasharchive.openarchive.onboarding.OAAppIntro;
import net.opendasharchive.openarchive.projects.AddProjectActivity;
import net.opendasharchive.openarchive.publish.UploadManagerActivity;
import net.opendasharchive.openarchive.ui.BadgeDrawable;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.util.Prefs;
import net.opendasharchive.openarchive.util.SelectiveViewPager;
import net.opendasharchive.openarchive.util.Utility;

import org.witness.proofmode.ProofMode;
import org.witness.proofmode.crypto.HashUtils;
import org.witness.proofmode.crypto.PgpUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import info.guardianproject.netcipher.proxy.OrbotHelper;

import static io.scal.secureshareui.controller.SiteController.MESSAGE_KEY_MEDIA_ID;
import static io.scal.secureshareui.controller.SiteController.MESSAGE_KEY_PROGRESS;
import static io.scal.secureshareui.controller.SiteController.MESSAGE_KEY_STATUS;
import static net.opendasharchive.openarchive.util.Globals.REQUEST_FILE_IMPORT;
import static net.opendasharchive.openarchive.util.Utility.getOutputMediaFile;


public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {

    private static final String TAG = "OASAVE:Main";
    private SelectiveViewPager mPager;
    private ProjectAdapter mPagerAdapter;

    private FloatingActionButton mFab;
    private ImageView mAvatar;
    private TextView mTitle;

    private int lastTab = 0;

    private MenuItem mMenuUpload;

    private Space mSpace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //otherwise go right into this app;

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        mTitle = findViewById(R.id.space_name);
        mAvatar = findViewById(R.id.space_avatar);
        mAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSpaceSettings();
            }
        });

        mPager = findViewById(R.id.pager);
        mPagerAdapter = new ProjectAdapter(this,getSupportFragmentManager());
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        if (mSpace != null) {
            List<Project> listProjects = Project.getAllBySpace(mSpace.getId(), false);
            mPagerAdapter.updateData(listProjects);
            mPager.setAdapter(mPagerAdapter);
            if (listProjects.size() > 0)
                mPager.setCurrentItem(1);
            else
                mPager.setCurrentItem(0);

            tabLayout.removeOnTabSelectedListener(this);
        }
        else {
            mPager.setAdapter(mPagerAdapter);
            mPager.setCurrentItem(0);

            tabLayout.addOnTabSelectedListener(this);


        }

           // final int pageMargin = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 8, getResources() .getDisplayMetrics());
        mPager.setPageMargin(0);


        mFab = (FloatingActionButton) findViewById(R.id.floating_menu);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mPagerAdapter.getCount() > 1 && lastTab > 0)
                    importMedia();
                else {
                    promptAddProject();
                }
            }
        });



        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {


            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                lastTab = position;

                if (position == 0)
                    promptAddProject();

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabLayout.setupWithViewPager(mPager);

        //check for any queued uploads and restart
        ((OpenArchiveApp)getApplication()).uploadQueue();



    }



    @Override
    public void onTabSelected(TabLayout.Tab tab) {


    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

        if (mSpace == null || mPagerAdapter.getCount() <= 1)
            promptAddProject();

    }


    public final static int REQUEST_NEW_PROJECT_NAME = 1001;

    public void promptAddProject ()
    {
        startActivityForResult(new Intent(this, AddProjectActivity.class),REQUEST_NEW_PROJECT_NAME);

    }

    public void onNewProjectClicked (View view)
    {
        promptAddProject();
    }

    private void refreshProjects ()
    {
        if (mSpace != null) {
            List<Project> listProjects = Project.getAllBySpace(mSpace.getId(), false);
            mPagerAdapter = new ProjectAdapter(this, getSupportFragmentManager());
            mPagerAdapter.updateData(listProjects);
            mPager.setAdapter(mPagerAdapter);

            if (listProjects.size() > 0)
                mPager.setCurrentItem(1);
            else
                mPager.setCurrentItem(0);

        }
        else
        {

        }

        updateMenu();

    }

    private void refreshCurrentProject ()
    {
        if (mPager.getCurrentItem() > 0) {
            MediaListFragment frag = ((MediaListFragment) mPagerAdapter.getRegisteredFragment(mPager.getCurrentItem()));
            if (frag != null)
                frag.refresh();
        }

        updateMenu ();

    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    private void setTitle (String title)
    {
        mTitle.setText(title);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(INTENT_FILTER_NAME));
        mAvatar.setImageResource(R.drawable.avatar_default);

        Space spaceCurrent = Space.getCurrentSpace();

        if (spaceCurrent != null)
        {
            if (mSpace != null) {

                List<Project> listProjects = Project.getAllBySpace(mSpace.getId(), false);

                if (mSpace.getId() != spaceCurrent.getId())
                {
                    initSpace(spaceCurrent);
                    refreshProjects();
                }
                else if (listProjects.size() != (mPagerAdapter.getCount()-1))
                {
                    initSpace(spaceCurrent);
                    refreshProjects();
                }
                else
                {
                    initSpace(spaceCurrent);
                }


            }
            else
            {
                initSpace(spaceCurrent);
                refreshProjects();
            }


            refreshCurrentProject();
        }

        if (mSpace == null || TextUtils.isEmpty(mSpace.host))
        {
            Intent intent = new Intent(this, OAAppIntro.class);
            startActivity(intent);
        }


        final Intent data = getIntent();
        importSharedMedia(data);

        if (mPager.getCurrentItem() == 0 && mPagerAdapter.getCount() > 1)
        {
            mPager.setCurrentItem(1);
        }

    }

    private void initSpace (Space space)
    {
        mSpace = space;

        if (mSpace != null &&(!TextUtils.isEmpty(mSpace.name)))
            setTitle(mSpace.name);
        else {

            Iterator<Space> listSpaces = Space.getAllAsList();
            if (listSpaces.hasNext())
            {
                mSpace = listSpaces.next();
                setTitle(mSpace.name);
                Prefs.setCurrentSpaceId(mSpace.getId());


            }
            else
                setTitle(R.string.main_activity_title);

        }

        if (mSpace.type == Space.TYPE_INTERNET_ARCHIVE) {
            mAvatar.setImageResource(R.drawable.ialogo128);
        }
        else
        {
            TextDrawable drawable = TextDrawable.builder()
                    .buildRound(mSpace.name.substring(0,1).toUpperCase(), getResources().getColor(R.color.oablue));
            mAvatar.setImageDrawable(drawable);
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        importSharedMedia(intent);
    }

    private void importSharedMedia (Intent data)
    {



        if (data != null) {

            final Snackbar bar = Snackbar.make(mPager, getString(R.string.importing_media), Snackbar.LENGTH_INDEFINITE);
            Snackbar.SnackbarLayout snack_view = (Snackbar.SnackbarLayout)bar.getView();
            snack_view.addView(new ProgressBar(this));
            // The Very Basic
            new AsyncTask<Void, Void, Media>() {
                protected void onPreExecute() {
                    bar.show();

                }
                protected Media doInBackground(Void... unused) {
                    return handleOutsideMedia(data);
                }
                protected void onPostExecute(Media media) {
                    // Post Code
                    if (media != null) {
                        Intent reviewMediaIntent = new Intent(MainActivity.this, ReviewMediaActivity.class);
                        reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, media.getId());
                        startActivity(reviewMediaIntent);
                    }

                    bar.dismiss();

                    setIntent(null);
                }
            }.execute();



            // handle if started from outside app
        }



    }

    public final static String INTENT_FILTER_NAME = "MEDIA_UPDATED";

    // Our handler for received Intents. This will be called whenever an Intent
// with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Log.d("receiver", "Updating media");
            long mediaId = intent.getLongExtra(MESSAGE_KEY_MEDIA_ID,-1);
            long progress = intent.getLongExtra(MESSAGE_KEY_PROGRESS,-1);

            int status = intent.getIntExtra(MESSAGE_KEY_STATUS,-1);
            if (status == Media.STATUS_UPLOADED) {
                if (mPager.getCurrentItem() > 0) {
                    MediaListFragment frag = ((MediaListFragment) mPagerAdapter.getRegisteredFragment(mPager.getCurrentItem()));
                    if (frag != null)
                        frag.refresh();

                    updateMenu();
                }
            }
            else if (status == (Media.STATUS_UPLOADING))
            {
                if (mediaId != -1 && mPager.getCurrentItem() > 0) {
                    MediaListFragment frag = ((MediaListFragment) mPagerAdapter.getRegisteredFragment(mPager.getCurrentItem()));
                    if (frag != null)
                        frag.updateItem(mediaId, progress);
                }

            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mMenuUpload = menu.findItem(R.id.menu_upload_manager);

        updateMenu();

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (item.getItemId()) {

            case android.R.id.home:
                showSpaceSettings();
                return true;
                /**
            case R.id.action_settings:
                Intent firstStartIntent = new Intent(this, SettingsActivity.class);
                startActivity(firstStartIntent);
                return true;
            case R.id.action_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;**/

            case R.id.menu_upload_manager:
                startActivity(new Intent(this, UploadManagerActivity.class));
                return true;


        }

        return super.onOptionsItemSelected(item);
    }

    private void updateMenu ()
    {
        if (mMenuUpload != null) {
            long[] mStatuses = {Media.STATUS_UPLOADING,Media.STATUS_QUEUED,Media.STATUS_ERROR};
            int uploadCount = Media.getMediaByStatus(mStatuses,Media.ORDER_PRIORITY).size();

            if (uploadCount > 0) {
                mMenuUpload.setVisible(true);

                BadgeDrawable bg = new BadgeDrawable(this);
                bg.setCount(uploadCount+"");
                mMenuUpload.setIcon(bg);

            } else
                mMenuUpload.setVisible(false);
        }
    }

    private void showSpaceSettings ()
    {
        Intent intent = new Intent(MainActivity.this, SpaceSettingsActivity.class);
        startActivity(intent);
    }

    private boolean mediaExists (Uri uri)
    {

        if (uri.getScheme() == null || uri.getScheme().equals("file"))
        {
            return new File(uri.getPath()).exists();
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        Log.d(TAG, "onActivityResult, requestCode:" + requestCode + ", resultCode: " + resultCode);

        // Check which request we're responding to
        if (requestCode == REQUEST_FILE_IMPORT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK && resultData != null) {

                final List<Uri> mSelected = Matisse.obtainResult(resultData);

                final Snackbar bar = Snackbar.make(mPager, R.string.importing_media, Snackbar.LENGTH_INDEFINITE);
                Snackbar.SnackbarLayout snack_view = (Snackbar.SnackbarLayout) bar.getView();
                snack_view.addView(new ProgressBar(this));

                new AsyncTask<List<Uri>, Void, List<Media>>() {
                    protected void onPreExecute() {
                        bar.show();
                    }

                    protected List<Media> doInBackground(List<Uri>... params) {
                        return importMedia(params[0]);
                    }

                    protected void onPostExecute(List<Media> media) {

                        bar.dismiss();

                        refreshCurrentProject();

                        if (media.size() > 0)
                        startActivity(new Intent(MainActivity.this, PreviewMediaListActivity.class));


                    }
                }.execute(mSelected);


            }
        } else if (requestCode == REQUEST_NEW_PROJECT_NAME) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {

                refreshProjects();
            }
        }


    }

    private ArrayList<Media> importMedia (List<Uri> importUri)
    {
        ArrayList<Media> result = new ArrayList<>();

        for (Uri uri: importUri) {
            Media media = importMedia(uri);
            if (media != null)
                result.add(media);
        }

        return result;
    }

    private Media importMedia (Uri uri)
    {
        if (uri == null)
            return null;

        String title = Utility.getUriDisplayName(this,uri);
        String mimeType = Utility.getMimeType(this,uri);

        File fileImport = getOutputMediaFile(this, title);
        try {
            boolean imported = Utility.writeStreamToFile(getContentResolver().openInputStream(uri),fileImport);
            if (!imported)
                return null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        Project project = mPagerAdapter.getProject(lastTab);

        if (project == null)
            return null;

        // create media
        Media media = new Media();

        Collection coll = null;

        if (project.getOpenCollectionId() == -1L)
        {
            coll = new Collection();
            coll.projectId = project.getId();
            coll.save();
            project.setOpenCollectionId(coll.getId());
            project.save();
        }
        else
        {
            coll = Collection.findById(Collection.class,project.getOpenCollectionId());

            if (coll == null
                    || coll.getUploadDate() != null)
            {
                coll = new Collection();
                coll.projectId = project.getId();
                coll.save();
                project.setOpenCollectionId(coll.getId());
                project.save();
            }
        }

        media.collectionId = coll.getId();

        File fileSource = new File(uri.getPath());
        Date createDate = new Date();
        if (fileSource.exists()) {
            createDate = new Date(fileSource.lastModified());
            media.contentLength = fileSource.length();
        }
        else
            media.contentLength = fileImport.length();

        media.setOriginalFilePath(Uri.fromFile(fileImport).toString());
        media.setMimeType(mimeType);
        media.setCreateDate(createDate);
        media.setUpdateDate(media.getCreateDate());
        media.status = Media.STATUS_LOCAL;

        media.mediaHashString =HashUtils.getSHA256FromFileContent(fileImport);
        media.projectId = project.getId();

        if (title != null)
            media.setTitle(title);
        media.save();

        return media;
    }

    private Media handleOutsideMedia(Intent intent) {

        Media media = null;

        if (intent != null && intent.getAction()!= null
          && intent.getAction().equals(Intent.ACTION_SEND)) {

            String mimeType = intent.getType();

            Uri uri = intent.getData();


            if (uri == null)
            {
                if (Build.VERSION.SDK_INT >= 16 && intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
                    uri = intent.getClipData().getItemAt(0).getUri();
                }
                else {
                    return null;
                }
            }


            media = importMedia(uri);

        }

        return media;
    }



    private void importMedia ()
    {
        if (!askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE,1)) {

            /**
            Intent intent = new Intent(this, Gallery.class);

// Set the title for toolbar
            intent.putExtra("title", getString(R.string.menu_import_media));
// Mode 1 for both images and videos selection, 2 for images only and 3 for videos!
            intent.putExtra("mode", 1);
            startActivityForResult(intent, Globals.REQUEST_FILE_IMPORT);
            **/

            Matisse.from(MainActivity.this)
                    .choose(MimeType.ofAll(),false)
                    .countable(true)
                    .maxSelectable(100)
              //      .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
              //      .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
                    .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                    .thumbnailScale(0.85f)
                    .imageEngine(new PicassoEngine())
                    .capture(true)
                    .captureStrategy(new CaptureStrategy(true, getPackageName() + ".provider", "capture"))
                    .forResult(REQUEST_FILE_IMPORT);
        }
    }



    private boolean askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            }

            return true;
        }

        return false;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 2);
                break;
            case 2:
                importMedia ();
                break;
        }

    }




}
