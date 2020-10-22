package net.opendasharchive.openarchive.onboarding;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.util.Prefs;

import info.guardianproject.netcipher.proxy.OrbotHelper;

/**
 * Created by n8fr8 on 8/3/16.
 */
public class OAAppIntro extends AppIntro {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setFadeAnimation();

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        //addSlide(AppIntroFragment.newInstance(getString(R.string.title_welcome), getString(R.string.onboarding_intro), R.drawable.oafeature, getResources().getColor(R.color.oablue)));

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        CustomSlideBigText welcome = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
        welcome.setTitle(getString(R.string.onboarding_intro));
        welcome.setSubTitle(getString(R.string.app_tag_line));
        /**
        welcome.showButton(getString(R.string.action_get_started), new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getPager().setCurrentItem(1);
            }
        });**/

        addSlide(welcome);

        addSlide(CustomOnboardingScreen.newInstance(R.layout.custom_onboarding_main,getString(R.string.oa_title_1), getString(R.string.oa_subtitle_1), R.drawable.onboarding1));
        addSlide(CustomOnboardingScreen.newInstance(R.layout.custom_onboarding_main,getString(R.string.oa_title_2), getString(R.string.oa_subtitle_2), R.drawable.onboarding2));
        addSlide(CustomOnboardingScreen.newInstance(R.layout.custom_onboarding_main,getString(R.string.oa_title_3), getString(R.string.oa_subtitle_3), R.drawable.onboarding3));
        addSlide(CustomOnboardingScreen.newInstance(R.layout.custom_onboarding_main,getString(R.string.oa_title_4), getString(R.string.oa_subtitle_4), R.drawable.onboarding4));

       if (!OrbotHelper.isOrbotInstalled(this))
       {
           CustomOnboardingScreen cos = CustomOnboardingScreen.newInstance(R.layout.custom_onboarding_main,getString(R.string.onboarding_archive_over_tor), getString(R.string.onboarding_archive_over_tor_install_orbot), R.drawable.onboarding5);
           addSlide(cos);
           cos.enableButton(getString(R.string.action_install), new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   Prefs.setUseTor(true);
                   startActivity(OrbotHelper.getOrbotInstallIntent(OAAppIntro.this));
               }
           });

        }
       else {
           CustomOnboardingScreen cos = CustomOnboardingScreen.newInstance(R.layout.custom_onboarding_main,getString(R.string.onboarding_archive_over_tor), getString(R.string.archive_over_tor_enable_orbot), R.drawable.onboarding5);
           addSlide(cos);
           cos.enableButton(getString(R.string.action_enable), new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   Prefs.setUseTor(true);
                   OrbotHelper.requestStartTor(OAAppIntro.this);
                   finish();
                   startActivity(new Intent(OAAppIntro.this, SpaceSetupActivity.class));
               }
           });

       }

        setColorDoneText(getResources().getColor(R.color.oablue));
        // OPTIONAL METHODS
        // Override bar/separator color.
       // setBarColor(Color.parseColor("#3F51B5"));
       // setSeparatorColor(Color.parseColor("#2196F3"));
        setSeparatorColor(getResources().getColor(R.color.white));
        setBarColor(getResources().getColor(R.color.oablue));
        // Hide Skip/Done button.
        showSkipButton(false);

        setProgressButtonEnabled(true);


    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.


        finish();
        startActivity(new Intent(this, SpaceSetupActivity.class));
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }
}
