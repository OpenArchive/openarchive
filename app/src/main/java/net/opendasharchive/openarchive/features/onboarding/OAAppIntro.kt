package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.AppIntro
import info.guardianproject.netcipher.proxy.OrbotHelper
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.onboarding.CustomOnboardingScreen.Companion.newInstance
import net.opendasharchive.openarchive.features.onboarding.CustomSlideBigText.Companion.newInstance
import net.opendasharchive.openarchive.util.Prefs

/**
 * Created by n8fr8 on 8/3/16.
 */
class OAAppIntro : AppIntro() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setFadeAnimation()

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        //addSlide(AppIntroFragment.newInstance(getString(R.string.title_welcome), getString(R.string.onboarding_intro), R.drawable.oafeature, getResources().getColor(R.color.oablue)));

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        //addSlide(AppIntroFragment.newInstance(getString(R.string.title_welcome), getString(R.string.onboarding_intro), R.drawable.oafeature, getResources().getColor(R.color.oablue)));

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        val welcome = newInstance(R.layout.custom_slide_big_text)
        welcome.setTitle(getString(R.string.onboarding_intro))
        welcome.setSubTitle(getString(R.string.app_tag_line))

        addSlide(welcome)

        addSlide(
            newInstance(
                R.layout.custom_onboarding_main,
                getString(R.string.oa_title_1),
                getString(R.string.oa_subtitle_1),
                R.drawable.onboarding1
            )
        )
        addSlide(
            newInstance(
                R.layout.custom_onboarding_main,
                getString(R.string.oa_title_2),
                getString(R.string.oa_subtitle_2),
                R.drawable.onboarding2
            )
        )
        addSlide(
            newInstance(
                R.layout.custom_onboarding_main,
                getString(R.string.oa_title_3),
                getString(R.string.oa_subtitle_3),
                R.drawable.onboarding3
            )
        )
        addSlide(
            newInstance(
                R.layout.custom_onboarding_main,
                getString(R.string.oa_title_4),
                getString(R.string.oa_subtitle_4),
                R.drawable.onboarding4
            )
        )

        if (!OrbotHelper.isOrbotInstalled(this)) {
            val cos = newInstance(
                R.layout.custom_onboarding_main,
                getString(R.string.onboarding_archive_over_tor),
                getString(R.string.onboarding_archive_over_tor_install_orbot),
                R.drawable.onboarding5
            )
            addSlide(cos)
            cos.enableButton(getString(R.string.action_install), View.OnClickListener {
                Prefs.setUseTor(true)
                startActivity(OrbotHelper.getOrbotInstallIntent(this@OAAppIntro))
            })
        } else {
            val cos = newInstance(
                R.layout.custom_onboarding_main,
                getString(R.string.onboarding_archive_over_tor),
                getString(R.string.archive_over_tor_enable_orbot),
                R.drawable.onboarding5
            )
            addSlide(cos)
            cos.enableButton(getString(R.string.action_enable), View.OnClickListener {
                Prefs.setUseTor(true)
                OrbotHelper.requestStartTor(this@OAAppIntro)
                finish()
                startActivity(Intent(this@OAAppIntro, SpaceSetupActivity::class.java))
            })
        }

        setColorDoneText(ContextCompat.getColor(this, R.color.white))
        setSeparatorColor(ContextCompat.getColor(this, R.color.white))
        setBarColor(ContextCompat.getColor(this, R.color.oablue))
        // Hide Skip/Done button.
        showSkipButton(false)

        isProgressButtonEnabled = true
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Do something when users tap on Skip button.
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Do something when users tap on Done button.
        finish()
        startActivity(Intent(this, SpaceSetupActivity::class.java))
    }

    override fun onSlideChanged(oldFragment: Fragment?, newFragment: Fragment?) {
        super.onSlideChanged(oldFragment, newFragment)
        // Do something when the slide changes.
    }

}