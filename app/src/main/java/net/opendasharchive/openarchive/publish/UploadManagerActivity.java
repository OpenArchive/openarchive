package net.opendasharchive.openarchive.publish;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.fragments.MediaListFragment;

import static io.scal.secureshareui.controller.SiteController.MESSAGE_KEY_MEDIA_ID;
import static io.scal.secureshareui.controller.SiteController.MESSAGE_KEY_PROGRESS;
import static io.scal.secureshareui.controller.SiteController.MESSAGE_KEY_STATUS;
import static net.opendasharchive.openarchive.MainActivity.INTENT_FILTER_NAME;


public class UploadManagerActivity extends AppCompatActivity {

    MediaListFragment mFrag;
    MenuItem mMenuEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_manager);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.title_uploads));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFrag = (MediaListFragment)getSupportFragmentManager().findFragmentById(R.id.fragUploadManager);
    }


    @Override
    protected void onResume() {
        super.onResume();

        mFrag.refresh();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(INTENT_FILTER_NAME));
    }


    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }


    // Our handler for received Intents. This will be called whenever an Intent
// with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Log.d("receiver", "Updating media");

            int status = intent.getIntExtra(MESSAGE_KEY_STATUS,-1);
            if (status == (Media.STATUS_UPLOADED))
                mFrag.refresh();
            else if (status == (Media.STATUS_UPLOADING))
            {
                long mediaId = intent.getLongExtra(MESSAGE_KEY_MEDIA_ID,-1);
                long progress = intent.getLongExtra(MESSAGE_KEY_PROGRESS,-1);

                if (mediaId != -1)
                {
                    mFrag.updateItem(mediaId, progress);
                }

            }

        }
    };

    boolean isEditMode = false;

    public void toggleEditMode ()
    {
        isEditMode = !isEditMode;
        mFrag.setEditMode(isEditMode);
        mFrag.refresh();

        if (isEditMode) {
            mMenuEdit.setTitle(R.string.menu_done);
            stopService(new Intent(this, PublishService.class));

        }
        else {
            mMenuEdit.setTitle(R.string.menu_edit);
            startService(new Intent(this, PublishService.class));


        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_upload, menu);
        mMenuEdit = menu.findItem(R.id.menu_edit);
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
                finish();
                return true;


            case R.id.menu_edit:
                toggleEditMode();
                return true;


        }

        return super.onOptionsItemSelected(item);
    }
}
