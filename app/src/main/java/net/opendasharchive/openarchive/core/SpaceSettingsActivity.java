package net.opendasharchive.openarchive.core;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.abdularis.civ.AvatarImageView;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.db.ProjectListAdapter;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.onboarding.SpaceSetupActivity;
import net.opendasharchive.openarchive.services.archivedotorg.ArchiveOrgLoginActivity;
import net.opendasharchive.openarchive.services.dropbox.DropboxLoginActivity;
import net.opendasharchive.openarchive.services.webdav.WebDAVLoginActivity;
import net.opendasharchive.openarchive.util.Prefs;

import java.util.Iterator;
import java.util.List;


public class SpaceSettingsActivity extends AppCompatActivity {

    RecyclerView mProjectList;
    Space mSpace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_space_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProjectList = findViewById(R.id.listProjects);
        mProjectList.setLayoutManager(new LinearLayoutManager(this));
        mProjectList.setHasFixedSize(false);


        findViewById(R.id.section_space).setOnClickListener(v -> startSpaceAuthActivity());

        findViewById(R.id.btnDataUse).setOnClickListener(v -> {
            Intent intent = new Intent(SpaceSettingsActivity.this, SettingsActivity.class);
            intent.putExtra(SettingsActivity.KEY_TYPE,SettingsActivity.KEY_DATAUSE);
            startActivity(intent);

        });

        findViewById(R.id.btnMetadata).setOnClickListener(v -> {
            Intent intent = new Intent(SpaceSettingsActivity.this, SettingsActivity.class);
            intent.putExtra(SettingsActivity.KEY_TYPE,SettingsActivity.KEY_METADATA);
            startActivity(intent);

        });

        findViewById(R.id.btnNetworking).setOnClickListener(v -> {
            Intent intent = new Intent(SpaceSettingsActivity.this, SettingsActivity.class);
            intent.putExtra(SettingsActivity.KEY_TYPE,SettingsActivity.KEY_NETWORKING);
            startActivity(intent);

        });

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;

            StringBuffer appString = new StringBuffer();
            appString.append(getString(R.string.app_name));
            appString.append(' ');
            appString.append(version);
            ((TextView)findViewById(R.id.txtVersion)).setText(appString.toString());

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }


    private void loadSpaces ()
    {
        Iterator<Space> listSpaces = Space.getAllAsList();

        LinearLayout viewSpace = findViewById(R.id.spaceview);
        viewSpace.removeAllViews();

        int actionBarHeight = 80;

        // Calculate ActionBar height
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
        {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }


        while (listSpaces.hasNext()) {
            Space space = listSpaces.next();

            ImageView image = getSpaceIcon(space, (int) (((float)actionBarHeight)*.7f));

            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Prefs.setCurrentSpaceId(space.getId());
                    showCurrentSpace();
                }
            });

            viewSpace.addView(image);
        }
    }

    private ImageView getSpaceIcon (Space space, int iconSize)
    {
        AvatarImageView image = new AvatarImageView(this);

        image.setAvatarBackgroundColor(getResources().getColor(R.color.oablue));

        if (space.type == Space.TYPE_INTERNET_ARCHIVE) {
            image.setImageResource(R.drawable.ialogo128);
            image.setState(AvatarImageView.SHOW_IMAGE);
        }
        else {
            if (TextUtils.isEmpty(space.name))
                space.name = space.username;

            image.setText(space.name.substring(0, 1).toUpperCase());
            image.setState(AvatarImageView.SHOW_INITIAL);
        }

        int margin = 6;

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(margin, margin, margin, margin);
        lp.height =iconSize;
        lp.width = iconSize;
        image.setLayoutParams(lp);

        return image;
    }

    @Override
    protected void onResume() {
        super.onResume();

        showCurrentSpace();
        loadSpaces();

        checkSpaceLink();
    }

    private void showCurrentSpace () {

        mSpace = Space.getCurrentSpace();
        if (mSpace == null)
        {
            Iterator<Space> listSpaces = Space.getAllAsList();
            if (listSpaces.hasNext())
            {
                mSpace = listSpaces.next();
                Prefs.setCurrentSpaceId(mSpace.getId());
            }
        }

        TextView txtSpaceName = findViewById(R.id.txtSpaceName);
        TextView txtSpaceUser = findViewById(R.id.txtSpaceUser);

        if (mSpace != null) {
            Uri uriServer = Uri.parse(mSpace.host);

            if (!TextUtils.isEmpty(mSpace.name))
                txtSpaceName.setText(mSpace.name);
            else
                txtSpaceName.setText(uriServer.getHost());

            txtSpaceUser.setText(mSpace.username);

            updateProjects();

            AvatarImageView image = findViewById(R.id.space_avatar);
            image.setAvatarBackgroundColor(getResources().getColor(R.color.oablue));
            if (mSpace.type == Space.TYPE_INTERNET_ARCHIVE) {
                image.setImageResource(R.drawable.ialogo128);
                image.setState(AvatarImageView.SHOW_IMAGE);

            }
            else {
                String spaceName = mSpace.name;
                if (TextUtils.isEmpty(spaceName))
                    spaceName = mSpace.host;

                image.setText(spaceName.substring(0, 1).toUpperCase());
                image.setState(AvatarImageView.SHOW_INITIAL);
            }

            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   startSpaceAuthActivity ();

                }
            });


        }
    }

    private void startSpaceAuthActivity ()
    {
        if (mSpace != null) {

            Intent intent = null;

            if (mSpace.type == Space.TYPE_WEBDAV)
                intent = new Intent(SpaceSettingsActivity.this, WebDAVLoginActivity.class);
            else if (mSpace.type == Space.TYPE_DROPBOX)
                intent = new Intent(SpaceSettingsActivity.this, DropboxLoginActivity.class);
            else
                intent = new Intent(SpaceSettingsActivity.this, ArchiveOrgLoginActivity.class);

            intent.putExtra("space", mSpace.getId());

            startActivity(intent);
        }
        else
            finish();
    }

    public void updateProjects ()
    {
        List<Project> listProjects = Project.getAllBySpace(Space.getCurrentSpace().getId());

        ProjectListAdapter adapter = new ProjectListAdapter(this,listProjects, mProjectList);

        mProjectList.setAdapter(adapter);
    }

    public void onAboutClick (View view)
    {
       // startActivity(new Intent(SpaceSettingsActivity.this, OAAppIntro.class));
        openBrowser("https://open-archive.org/about/");
    }

    public void onPrivacyClick (View view)
    {
        openBrowser("https://open-archive.org/privacy/");

    }

    private void openBrowser (String link)
    {
        try {
            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No application can handle this request."
                    + " Please install a webbrowser",  Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onNewSpaceClicked (View view)
    {
        Intent myIntent = new Intent(this, SpaceSetupActivity.class);
        startActivity(myIntent);
    }

    private void checkSpaceLink ()
    {
        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();

            String queryString = uri.toString();

            if(queryString != null && queryString.startsWith("nc://login/"))
            {
                //user, password, server
                String[] queryParts = queryString.substring(11).split("&");

                String user = queryParts[0].substring(5);
                String password = queryParts[1].substring(9);
                String server = queryParts[2].substring(7);

                Intent intentLogin = new Intent(this, WebDAVLoginActivity.class);
                intentLogin.putExtra("user",user);
                intentLogin.putExtra("password",password);
                intentLogin.putExtra("server",server);

                startActivity(intentLogin);

            }


        }
    }
}
