package net.opendasharchive.openarchive.onboarding;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.onboarding.EulaActivity.OnEulaAgreedTo;
import net.opendasharchive.openarchive.services.archivedotorg.ArchiveOrgLoginActivity;
import net.opendasharchive.openarchive.services.dropbox.DropboxLoginActivity;
import net.opendasharchive.openarchive.services.webdav.WebDAVLoginActivity;

import java.util.Iterator;

import io.scal.secureshareui.controller.SiteController;

/**
 * Prompt the user to view & agree to the  TOS / EULA
 * and present the choice to create a Account.
 * <p/>
 * Should be launched as the start of a new Task, because
 * when this Activity finishes without starting another,
 * it is the result of the user rejecting the TOS / EULA,
 * and so should result in the app exiting.
 *
 * @author micahjlucas
 */
public class SpaceSetupActivity extends Activity implements OnEulaAgreedTo {

    private static final String TAG = "FirstStartActivity";
    private boolean eulaAgreed = false;
    private Space mSpace = null;

    private SiteController.OnEventListener mAuthEventListener = new SiteController.OnEventListener() {

        @Override
        public void onSuccess(Space space) {

            space.save();
        }

        @Override
        public void onFailure(Space space, String failureMessage) {

        }

        @Override
        public void onRemove(Space space) {

        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_sign_in);
        mSpace = new Space();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean hasDropbox = false;

        Iterator<Space> itSpaces = Space.getAllAsList();

        while (itSpaces.hasNext()) {
            Space space = itSpaces.next();
            hasDropbox = space.type == Space.TYPE_DROPBOX;
        }

        findViewById(R.id.dropboxFrame).setVisibility(hasDropbox ? View.GONE : View.VISIBLE);
    }

    public void onSignInArchiveButtonClick (View v) {
        startActivity(new Intent(this, ArchiveOrgLoginActivity.class));
        finish();
    }


    public void onSignInPrivateButtonClick (View v) {

        startActivity(new Intent(this, WebDAVLoginActivity.class));
        finish();
    }

    public void onSetupDropboxButtonClick (View v) {
        startActivity(new Intent(this, DropboxLoginActivity.class));
        finish();

    }

    /**
    private void doAuthentication ()
    {
        boolean useTor = ((OpenArchiveApp)getApplication()).getUseTor();

        SiteController siteController = SiteController.getSiteController(ArchiveSiteController.SITE_KEY, this, null, null);
        siteController.setUseTor(useTor);
        siteController.setOnEventListener(mAuthEventListener);

        siteController.startAuthentication(mAccount);
    }

    private void doSignUp ()
    {
        boolean useTor = ((OpenArchiveApp)getApplication()).getUseTor();
        SiteController siteController = SiteController.getSiteController(ArchiveSiteController.SITE_KEY, this, null, null);
        siteController.setUseTor(useTor);
        siteController.setOnEventListener(mAuthEventListener);
        siteController.startRegistration(mAccount);
    }**/

    /**
     * Show an AlertDialog prompting the user to
     * accept the EULA / TOS
     *
     * @return
     */
    private boolean assertTosAccepted() {
        return new EulaActivity(this).show();

    }


    @Override
    public void onEulaAgreedTo() {
        //doAuthentication ();

    }

    /**
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == SiteController.CONTROLLER_REQUEST_CODE) {
            if (resultCode == android.app.Activity.RESULT_OK) {

                mAccount = new Account(this, null);
                String credentials = intent.getStringExtra(SiteController.EXTRAS_KEY_CREDENTIALS);
                mAccount.setCredentials(credentials != null ? credentials : "");

                String username = intent.getStringExtra(SiteController.EXTRAS_KEY_USERNAME);
                mAccount.setUserName(username != null ? username : "");

                String data = intent.getStringExtra(SiteController.EXTRAS_KEY_DATA);
                mAccount.setData(data != null ? data : null);

                mAccount.setAuthenticated(true);
                mAccount.saveToSharedPrefs(this, null);

                Intent mainIntent = new Intent(this, MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainIntent);

            }
        }
    }**/

}

