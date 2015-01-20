package io.scal.openarchive;

import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.IOException;

import io.scal.openarchive.db.Media;
import io.scal.secureshareui.model.Account;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        FragmentMain.OnFragmentInteractionListener{

    private static final String TAG = "MainActivity";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;

    //FIXME
    public static boolean SHOULD_SPIN = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if user has not accepted EULA
        if(!EulaActivity.isAccepted(this)) {
            Intent firstStartIntent = new Intent(this, FirstStartActivity.class);
            firstStartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(firstStartIntent);
            finish();
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isFirstRun = sp.getBoolean(Globals.PREF_FIRST_RUN, true);
        // if first time running app
        if (isFirstRun) {
            initFirstRun(sp);
        }

        Account account = new Account(this, null);
        Toast.makeText(this, "Username: " + account.getUserName() + ", credentials: " + account.getCredentials(), Toast.LENGTH_LONG).show();

        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // set up nav drawer
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        //FIXME REMOVE
        if(SHOULD_SPIN) {
            SHOULD_SPIN = false;

            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(getString(R.string.loading_title));
            progressDialog.setMessage(getString(R.string.loading_message));
            progressDialog.show();

            Thread progressThread = new Thread(){
                @Override
                public void run(){
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        progressDialog.dismiss();
                    }
                }
            };
            progressThread.start();
        }

        // handle if started from outside app
        handleOutsideIntent(getIntent());
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        switch (position) {
            case 0: //home
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, FragmentMain.newInstance())
                        .commit();
                break;
            case 1: //view uploads
                Intent mediaListIntent = new Intent(this, MediaListActivity.class);
                startActivity(mediaListIntent);
                break;
            case 2: //logout
                Toast.makeText(getApplicationContext(), "Logout", Toast.LENGTH_SHORT).show();
                break;
            case 3: //settings
                Intent settingsIntent = new Intent(this, ArchiveSettingsActivity.class);
                startActivity(settingsIntent);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, ArchiveSettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult, requestCode:" + requestCode + ", resultCode: " + resultCode);

        String path = null;
        Media.MEDIA_TYPE mediaType = null;

        if (resultCode == RESULT_OK) {
            if(requestCode == Globals.REQUEST_AUDIO_CAPTURE) {
                Uri uri = intent.getData();
                path = Utility.getRealPathFromURI(getApplicationContext(), uri);
                mediaType = Media.MEDIA_TYPE.AUDIO;

                Log.d(TAG, "onActivityResult, audio path:" + path);

            } else if(requestCode == Globals.REQUEST_IMAGE_CAPTURE) {
                path = this.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString(Globals.EXTRA_FILE_LOCATION, null);
                mediaType = Media.MEDIA_TYPE.IMAGE;

                Log.d(TAG, "onActivityResult, image path:" + path);

            } else if(requestCode == Globals.REQUEST_VIDEO_CAPTURE) {
                Uri uri = intent.getData();
                path = Utility.getRealPathFromURI(getApplicationContext(), uri);
                mediaType = Media.MEDIA_TYPE.VIDEO;

                Log.d(TAG, "onActivityResult, video path:" + path);

            }  else if (requestCode == Globals.REQUEST_FILE_IMPORT) {
                Uri uri = intent.getData();
                // Will only allow stream-based access to files
                if (Build.VERSION.SDK_INT >= 19) {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                path = uri.toString();
                mediaType = Utility.getMediaType(path);

                Log.d(TAG, "onActivityResult, imported file path:" + path);
            }

            if (null == path) {
                Log.d(TAG, "onActivityResult: Invalid file on import or capture");
                Toast.makeText(getApplicationContext(), R.string.error_file_not_found, Toast.LENGTH_SHORT).show();
            } else if (null == mediaType) {
                Log.d(TAG, "onActivityResult: Invalid Media Type");
                Toast.makeText(getApplicationContext(), R.string.error_invalid_media_type, Toast.LENGTH_SHORT).show();
            } else {
                // create media
                Media media = new Media(path, mediaType);

                Intent reviewMediaIntent = new Intent(this, ReviewMediaActivity.class);
                reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, media.getId());
                startActivity(reviewMediaIntent);
            }
        }
    }

    private void initFirstRun(SharedPreferences sp) {
        // set first run flag as false
        sp.edit().putBoolean(Globals.PREF_FIRST_RUN, false).apply();
    }


    private void handleOutsideIntent(Intent intent) {
        // Get intent, action and MIME type
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            handleSendImage(intent);

            if (type.startsWith("image/")) {
                handleSendImage(intent); // handle single image being sent
            }
        }
    }

    void handleSendImage(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri != null) {

            String path = null;
            Media.MEDIA_TYPE mediaType = null;

            path = uri.toString();

            path = Utility.getRealPathFromURI(getApplicationContext(), uri);
            mediaType = Utility.getMediaType(path);

            Uri imageUri = intent.getData();
            Bitmap bitmap;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                int i =1;
            } catch (IOException e) {
                e.printStackTrace();
            }


            // create media
            Media media = new Media(path, mediaType);

            Intent reviewMediaIntent = new Intent(this, ReviewMediaActivity.class);
            reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, media.getId());
            startActivity(reviewMediaIntent);
        }
    }
}
