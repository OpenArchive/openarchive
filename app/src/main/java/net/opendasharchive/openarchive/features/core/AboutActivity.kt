package net.opendasharchive.openarchive.features.core

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import net.opendasharchive.openarchive.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity(){

    private lateinit var mBinding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.aboutView.movementMethod = LinkMovementMethod.getInstance()
    }

}