package net.opendasharchive.openarchive.features.projects

import android.R
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import net.opendasharchive.openarchive.databinding.ActivityAddProjectBinding
import net.opendasharchive.openarchive.db.Space.Companion.getCurrentSpace
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.projects.BrowseProjectsActivity
import net.opendasharchive.openarchive.util.Constants.EMPTY_STRING

class AddProjectActivity : AppCompatActivity() {

    private val CREATE_NEW_PROJECT_CODE = 1000
    private val BROWSE_PROJECTS_CODE = 1001

    private lateinit var mBinding: ActivityAddProjectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityAddProjectBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initLayout()
    }

    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = EMPTY_STRING
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun onNewProjectClicked(view: View?) {
        val space = getCurrentSpace()
        if (space != null) {
            val intent = Intent(this, CreateNewProjectActivity::class.java)
            startActivityForResult(intent, CREATE_NEW_PROJECT_CODE)
        } else {
            finish()
            startActivity(Intent(this, SpaceSetupActivity::class.java))
        }
    }

    fun onBrowseProjects(view: View?) {
        val space = getCurrentSpace()
        if (space != null) {
            val intent = Intent(this, BrowseProjectsActivity::class.java)
            startActivityForResult(intent, BROWSE_PROJECTS_CODE)
        } else {
            finish()
            startActivity(Intent(this, SpaceSetupActivity::class.java))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }
}